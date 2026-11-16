package com.minewaku.chatter.profilenext.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.minewaku.chatter.profilenext.start.support.ImageE2eSupport;
import com.minewaku.chatter.profilenext.start.support.ImageFixtures;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AccountDeletionE2eTest extends ImageE2eSupport {

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private long userWithBothImages() throws Exception {
        long user = newUser();
        assertThat(send("PATCH", "/api/v1/profiles/@me", bearer(user), "{\"displayName\":\"Mine\",\"bio\":\"Hi\"}")
                        .statusCode())
                .isEqualTo(204);
        setImage(user, "avatar", 512);
        setImage(user, "banner", 256);
        return user;
    }

    @Test
    void aNoticeRemovesTheProfileAndTheSweeperDeletesBothImages() throws Exception {
        long user = userWithBothImages();
        assertThat(storedKeys(user)).hasSize(2);

        publishNotice(user, freshEventId(), 1);

        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(rows(user)).isZero();
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
        assertThat(profileOf(user))
                .contains("\"displayName\":\"\"")
                .contains("\"avatarUrl\":null")
                .contains("\"createdAt\":null");
    }

    @Test
    void aRepeatedNoticeAndANoticeForAUserWhoNeverHadAProfileChangeNothingAndRaiseNothing() throws Exception {
        long never = newUser();
        long user = userWithBothImages();
        String eventId = freshEventId();

        publishNotice(never, freshEventId(), 1);
        publishNotice(user, eventId, 3);

        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(rows(user)).isZero();
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
        assertThat(rows(never)).isZero();
    }

    @Test
    void whenStorageFailsTheRowIsStillRemovedAndTheFilesAreDeletedLater() throws Exception {
        long user = userWithBothImages();
        STORAGE.failing = true;

        publishNotice(user, freshEventId(), 1);

        await().atMost(PATIENCE).until(() -> rows(user) == 0);
        assertThat(storedKeys(user)).hasSize(2);
        assertThat(ledgerRows(user)).isEqualTo(2);

        STORAGE.failing = false;

        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
    }

    @Test
    void anUploadThatWasUnderWayWhenTheAccountWasDeletedCannotBeCommittedAndIsStillCleanedUp() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(512, 512));

        publishNotice(user, freshEventId(), 1);
        await().atMost(PATIENCE).until(() -> ledgerRows(user) == 1 && pendingTickets(user) == 0);

        HttpResponse<String> late = commit(user, "avatar", imageId);

        assertThat(late.statusCode()).isEqualTo(404);
        assertThat(rows(user)).isZero();
        assertThat(storedKeys(user)).hasSize(1);

        // its sweep time was kept, so once that has passed the file goes too
        jdbc.sql("UPDATE pending_upload SET delete_after = now() - interval '1 second' WHERE user_id = :id")
                .param("id", user)
                .update();
        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
    }

    private long pendingTickets(long user) {
        return jdbc.sql("SELECT count(*) FROM pending_upload WHERE user_id = :id AND state = 'PENDING'")
                .param("id", user)
                .query(Long.class)
                .single();
    }

    @Test
    void aStillValidTokenForTheDeletedUserIsAcceptedForReads() throws Exception {
        long user = userWithBothImages();

        publishNotice(user, freshEventId(), 1);
        await().atMost(PATIENCE).until(() -> rows(user) == 0);

        HttpResponse<String> response = send("GET", "/api/v1/profiles/@me", bearer(user), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"displayName\":\"\"");
    }
}
