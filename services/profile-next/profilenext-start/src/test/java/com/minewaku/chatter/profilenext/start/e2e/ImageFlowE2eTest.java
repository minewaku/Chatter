package com.minewaku.chatter.profilenext.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.minewaku.chatter.profilenext.profile.api.command.SweepUnreferencedImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.start.support.FakeCloudStorage;
import com.minewaku.chatter.profilenext.start.support.StartTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class ImageFlowE2eTest extends StartTestSupport {

    private static final AtomicLong IDS = new AtomicLong(6_000_000L);

    // UUIDv7-shaped ids that order by their first group, as the production generator's do
    private static String uploadId(int order) {
        return String.format("0197%04x-0000-7000-8000-%012d", order, order);
    }

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SweepUnreferencedImagesUseCase sweep;

    @BeforeEach
    void reset() {
        STORAGE.failing = false;
        STORAGE.files.clear();
        jdbc.sql("DELETE FROM pending_image_removal").update();
    }

    private HttpResponse<String> notify(String body, String signature) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/v1/profiles/uploads/notifications")))
                .header("Content-Type", "application/json")
                .header("X-Cld-Signature", signature)
                .header("X-Cld-Timestamp", Long.toString(System.currentTimeMillis() / 1000))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Upload to the provider, then deliver its confirmation. */
    private HttpResponse<String> uploadAndConfirm(long user, ImageSlot slot, String uploadId) throws Exception {
        STORAGE.upload(user, slot, uploadId);
        String body = FakeCloudStorage.body(user, slot, uploadId, "png", 2048);
        return notify(body, FakeCloudStorage.signatureFor(body));
    }

    private String profileOf(long user) throws Exception {
        return send("GET", "/api/v1/profiles/" + user, bearer(user), null).body();
    }

    private Integer attemptsFor(String publicId) {
        return jdbc.sql("SELECT attempts FROM pending_image_removal WHERE public_id = :id")
                .param("id", publicId)
                .query(Integer.class)
                .optional()
                .orElse(null);
    }

    @Test
    void permissionThenConfirmationShowsTheNewImage() throws Exception {
        long user = IDS.incrementAndGet();

        HttpResponse<String> permission = send("POST", "/api/v1/profiles/avatar/upload-signature", bearer(user), null);
        assertThat(permission.statusCode()).isEqualTo(200);
        Matcher publicId = Pattern.compile("chatter/profile/avatars/" + user + "/([0-9a-f-]+)")
                .matcher(permission.body());
        assertThat(publicId.find()).isTrue();
        assertThat(permission.body()).contains("\"uploadUrl\"").contains("\"expiresAt\"");

        String uploadId = publicId.group(1);
        assertThat(uploadAndConfirm(user, ImageSlot.AVATAR, uploadId).statusCode()).isEqualTo(204);

        assertThat(profileOf(user))
                .contains("\"avatarUrl\":\"" + FakeCloudStorage.urlOf(FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId)) + "\"")
                .contains("\"bannerUrl\":null");
    }

    @Test
    void aSecondUploadQueuesTheFirstAndTheWorkerRemovesIt() throws Exception {
        long user = IDS.incrementAndGet();
        String first = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(1));
        String second = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(2));
        uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(1));

        assertThat(uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(2)).statusCode()).isEqualTo(204);

        assertThat(profileOf(user)).contains(FakeCloudStorage.urlOf(second));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(STORAGE.files.keySet()).containsExactly(second);
            assertThat(attemptsFor(first)).isNull();
        });
    }

    @Test
    void theSameConfirmationTwiceChangesNothing() throws Exception {
        long user = IDS.incrementAndGet();
        uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(1));
        String before = profileOf(user);

        assertThat(uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(1)).statusCode()).isEqualTo(204);

        assertThat(profileOf(user)).isEqualTo(before);
        assertThat(STORAGE.files).containsKey(FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(1)));
        assertThat(jdbc.sql("SELECT count(*) FROM pending_image_removal").query(Long.class).single()).isZero();
    }

    @Test
    void anInvalidSignatureIsRefusedAndChangesNothing() throws Exception {
        long user = IDS.incrementAndGet();
        String body = FakeCloudStorage.body(user, ImageSlot.AVATAR, uploadId(1), "png", 2048);
        STORAGE.upload(user, ImageSlot.AVATAR, uploadId(1));

        HttpResponse<String> response = notify(body, "sig:forged");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"unauthenticated\"");
        assertThat(profileOf(user)).contains("\"avatarUrl\":null");
    }

    @Test
    void aFileOverItsLimitIsForbiddenAndQueuedForRemoval() throws Exception {
        long user = IDS.incrementAndGet();
        STORAGE.failing = true; // keep the worker from clearing the row before it is observed
        STORAGE.upload(user, ImageSlot.AVATAR, uploadId(1));
        String body = FakeCloudStorage.body(user, ImageSlot.AVATAR, uploadId(1), "png", 6L * 1024 * 1024);

        HttpResponse<String> response = notify(body, FakeCloudStorage.signatureFor(body));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(profileOf(user)).contains("\"avatarUrl\":null");
        assertThat(attemptsFor(FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(1)))).isNotNull();
    }

    @Test
    void whenTheProviderFailsTheProfileStillShowsTheNewImageAndRemovalRetriesUntilItRecovers() throws Exception {
        long user = IDS.incrementAndGet();
        String first = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(1));
        String second = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(2));
        uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(1));
        STORAGE.failing = true;

        assertThat(uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(2)).statusCode()).isEqualTo(204);

        assertThat(profileOf(user)).contains(FakeCloudStorage.urlOf(second));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(attemptsFor(first)).isGreaterThanOrEqualTo(2));
        assertThat(STORAGE.files).containsKey(first);

        STORAGE.failing = false;

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(attemptsFor(first)).isNull();
            assertThat(STORAGE.files.keySet()).containsExactly(second);
        });
    }

    @Test
    void aNeverConfirmedOldFileIsRemovedBySweep() throws Exception {
        long user = IDS.incrementAndGet();
        STORAGE.upload(user, ImageSlot.AVATAR, UUID.randomUUID().toString());
        Thread.sleep(50); // the grace period is zero: the file just has to be older than "now"

        var result = sweep.handle(new SweepUnreferencedImagesUseCase.Command());

        assertThat(result.removed()).isEqualTo(1);
        assertThat(STORAGE.files).isEmpty();
    }

    @Test
    void sweepKeepsAFileThatAProfileStillShows() throws Exception {
        long user = IDS.incrementAndGet();
        uploadAndConfirm(user, ImageSlot.AVATAR, uploadId(1));
        Thread.sleep(50);

        sweep.handle(new SweepUnreferencedImagesUseCase.Command());

        assertThat(STORAGE.files).containsKey(FakeCloudStorage.publicId(user, ImageSlot.AVATAR, uploadId(1)));
    }
}
