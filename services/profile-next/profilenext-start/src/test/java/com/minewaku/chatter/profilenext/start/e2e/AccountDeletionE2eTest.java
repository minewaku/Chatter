package com.minewaku.chatter.profilenext.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.start.support.FakeCloudStorage;
import com.minewaku.chatter.profilenext.start.support.StartTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class AccountDeletionE2eTest extends StartTestSupport {

    private static final AtomicLong IDS = new AtomicLong(7_000_000L);
    private static final String AVATAR_UPLOAD = "0197aaaa-0000-7000-8000-000000000001";
    private static final String BANNER_UPLOAD = "0197aaab-0000-7000-8000-000000000002";

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void reset() {
        STORAGE.failing = false;
    }

    private static String notice(String eventId, long userId) {
        return "{\"eventId\":\"" + eventId + "\",\"userId\":\"" + userId + "\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}";
    }

    /** Publishes the way identity-access's relay does: key = user id, headers id and eventType. */
    private static void publishNotice(long userId, String eventId, int times) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < times; i++) {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(ACCOUNT_TOPIC, Long.toString(userId), notice(eventId, userId));
                record.headers().add("id", eventId.getBytes(StandardCharsets.UTF_8));
                record.headers().add("eventType", "UserSoftDeleted".getBytes(StandardCharsets.UTF_8));
                producer.send(record);
            }
            producer.flush();
        }
    }

    private HttpResponse<String> confirm(long user, ImageSlot slot, String uploadId) throws Exception {
        STORAGE.upload(user, slot, uploadId);
        String body = FakeCloudStorage.body(user, slot, uploadId, "png", 1024);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/v1/profiles/uploads/notifications")))
                .header("Content-Type", "application/json")
                .header("X-Cld-Signature", FakeCloudStorage.signatureFor(body))
                .header("X-Cld-Timestamp", Long.toString(System.currentTimeMillis() / 1000))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long userWithBothImages() throws Exception {
        long user = IDS.incrementAndGet();
        assertThat(send("PATCH", "/api/v1/profiles/@me", bearer(user), "{\"displayName\":\"Mine\",\"bio\":\"Hi\"}")
                        .statusCode())
                .isEqualTo(204);
        assertThat(confirm(user, ImageSlot.AVATAR, AVATAR_UPLOAD).statusCode()).isEqualTo(204);
        assertThat(confirm(user, ImageSlot.BANNER, BANNER_UPLOAD).statusCode()).isEqualTo(204);
        return user;
    }

    private long rows(long user) {
        return jdbc.sql("SELECT count(*) FROM profile WHERE user_id = :id")
                .param("id", user)
                .query(Long.class)
                .single();
    }

    private Integer attemptsFor(String publicId) {
        return jdbc.sql("SELECT attempts FROM pending_image_removal WHERE public_id = :id")
                .param("id", publicId)
                .query(Integer.class)
                .optional()
                .orElse(null);
    }

    private String profileOf(long user) throws Exception {
        return send("GET", "/api/v1/profiles/" + user, bearer(user), null).body();
    }

    @Test
    void aNoticeRemovesTheProfileAndTheWorkerRemovesBothImages() throws Exception {
        long user = userWithBothImages();
        String avatar = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, AVATAR_UPLOAD);
        String banner = FakeCloudStorage.publicId(user, ImageSlot.BANNER, BANNER_UPLOAD);
        assertThat(STORAGE.files).containsKeys(avatar, banner);

        publishNotice(user, "evt-" + UUID.randomUUID(), 1);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(rows(user)).isZero();
            assertThat(STORAGE.files).doesNotContainKeys(avatar, banner);
            assertThat(attemptsFor(avatar)).isNull();
            assertThat(attemptsFor(banner)).isNull();
        });
        assertThat(profileOf(user))
                .contains("\"displayName\":\"\"")
                .contains("\"avatarUrl\":null")
                .contains("\"createdAt\":null");
    }

    @Test
    void aRepeatedNoticeAndANoticeForAUserWhoNeverHadAProfileChangeNothingAndRaiseNothing() throws Exception {
        long user = userWithBothImages();
        long neverSaved = IDS.incrementAndGet();
        long marker = userWithBothImages();

        publishNotice(user, "evt-a", 1);
        await().atMost(Duration.ofSeconds(30)).until(() -> rows(user) == 0);
        publishNotice(user, "evt-a", 1);
        publishNotice(neverSaved, "evt-b", 1);
        publishNotice(marker, "evt-c", 1);

        // the listener is still alive: the later notice for another user is applied
        await().atMost(Duration.ofSeconds(30)).until(() -> rows(marker) == 0);
        assertThat(rows(user)).isZero();
        assertThat(rows(neverSaved)).isZero();
    }

    @Test
    void whenStorageFailsTheRowIsStillRemovedAndTheRemovalsRetryLater() throws Exception {
        long user = userWithBothImages();
        String avatar = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, AVATAR_UPLOAD);
        STORAGE.failing = true;

        publishNotice(user, "evt-d", 1);

        await().atMost(Duration.ofSeconds(30)).until(() -> rows(user) == 0);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(attemptsFor(avatar)).isGreaterThanOrEqualTo(1));
        assertThat(STORAGE.files).containsKey(avatar);

        STORAGE.failing = false;

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(attemptsFor(avatar)).isNull();
            assertThat(STORAGE.files).doesNotContainKey(avatar);
        });
    }

    @Test
    void aThousandRepeatsOfOneNoticeEndInTheSameStateAsOne() throws Exception {
        long user = userWithBothImages();
        String avatar = FakeCloudStorage.publicId(user, ImageSlot.AVATAR, AVATAR_UPLOAD);
        String banner = FakeCloudStorage.publicId(user, ImageSlot.BANNER, BANNER_UPLOAD);
        long marker = userWithBothImages();

        publishNotice(user, "evt-e", 1000);
        publishNotice(marker, "evt-marker", 1);

        await().atMost(Duration.ofSeconds(90)).until(() -> rows(marker) == 0);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(rows(user)).isZero();
            assertThat(STORAGE.files).doesNotContainKeys(avatar, banner);
            assertThat(attemptsFor(avatar)).isNull();
        });
        assertThat(jdbc.sql("SELECT count(*) FROM pending_image_removal WHERE public_id IN (:ids)")
                        .param("ids", java.util.List.of(avatar, banner))
                        .query(Long.class)
                        .single())
                .isZero();
    }

    @Test
    void aStillValidTokenForTheDeletedUserIsAcceptedForReads() throws Exception {
        long user = userWithBothImages();

        publishNotice(user, "evt-f", 1);
        await().atMost(Duration.ofSeconds(30)).until(() -> rows(user) == 0);

        HttpResponse<String> response = send("GET", "/api/v1/profiles/@me", bearer(user), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"displayName\":\"\"");
    }
}
