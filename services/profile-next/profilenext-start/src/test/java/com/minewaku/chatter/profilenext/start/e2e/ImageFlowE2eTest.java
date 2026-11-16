package com.minewaku.chatter.profilenext.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.minewaku.chatter.profilenext.start.support.ImageE2eSupport;
import com.minewaku.chatter.profilenext.start.support.ImageFixtures;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The whole image workflow against real PostgreSQL, the real adapter and use cases, and an in-memory object store. */
class ImageFlowE2eTest extends ImageE2eSupport {

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    @Test
    void ticketUploadCommitShowsTheImageAndLeavesNothingBehind() throws Exception {
        long user = newUser();

        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(512, 512));
        assertThat(profileOf(user)).contains("\"avatarUrl\":null");

        HttpResponse<String> committed = commit(user, "avatar", imageId);

        assertThat(committed.statusCode()).isEqualTo(200);
        assertThat(committed.body())
                .contains("\"outcome\":\"applied\"")
                .contains("\"imageUrl\":\"" + addressOf(user, "avatar", imageId) + "\"");
        assertThat(profileOf(user)).contains("\"avatarUrl\":\"" + addressOf(user, "avatar", imageId) + "\"");
        assertThat(storedKeys(user)).containsExactly(keyOf(user, "avatar", imageId));
        assertThat(ledgerRows(user)).isZero();
    }

    @Test
    void theTicketPointsStraightAtTheStorageAndSignsExactlyTheDeclaredFile() throws Exception {
        long user = newUser();

        HttpResponse<String> ticket = requestTicket(user, "banner", "image/webp", 4321);

        assertThat(ticket.statusCode()).isEqualTo(200);
        String imageId = string(ticket.body(), "imageId");
        assertThat(string(ticket.body(), "uploadUrl")).contains("/" + BUCKET + "/" + keyOf(user, "banner", imageId));
        assertThat(ticket.body()).contains("\"method\":\"PUT\"").contains("\"Content-Type\":\"image/webp\"").contains("\"Content-Length\":\"4321\"");
        assertThat(ledgerRows(user)).isEqualTo(1);
    }

    @Test
    void replacingAnImageSwitchesTheAddressAndTheOldFileIsDeleted() throws Exception {
        long user = newUser();
        String first = setImage(user, "avatar", 512);

        String second = setImage(user, "avatar", 256);

        assertThat(profileOf(user)).contains(addressOf(user, "avatar", second)).doesNotContain(first);
        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).containsExactly(keyOf(user, "avatar", second));
            assertThat(ledgerRows(user)).isZero();
        });
    }

    @Test
    void theTwoSlotsAreIndependent() throws Exception {
        long user = newUser();

        String avatar = setImage(user, "avatar", 512);
        String banner = upload(user, "banner", "image/png", ImageFixtures.png(1500, 500));
        assertThat(commit(user, "banner", banner).statusCode()).isEqualTo(200);

        assertThat(profileOf(user))
                .contains(addressOf(user, "avatar", avatar))
                .contains(addressOf(user, "banner", banner));
    }

    @Test
    void aRepeatedCommitIsAlreadyApplied() throws Exception {
        long user = newUser();
        String imageId = setImage(user, "avatar", 512);

        HttpResponse<String> again = commit(user, "avatar", imageId);

        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(again.body()).contains("\"outcome\":\"already_applied\"");
    }

    @Test
    void manyCommitsOfOneTicketAtOnceApplyItExactlyOnce() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(512, 512));
        AtomicInteger applied = new AtomicInteger();
        AtomicInteger alreadyApplied = new AtomicInteger();
        List<CompletableFuture<Void>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            calls.add(CompletableFuture.runAsync(() -> {
                try {
                    HttpResponse<String> response = commit(user, "avatar", imageId);
                    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
                    if (response.body().contains("\"outcome\":\"applied\"")) {
                        applied.incrementAndGet();
                    } else {
                        alreadyApplied.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        }
        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).join();

        assertThat(applied).hasValue(1);
        assertThat(alreadyApplied).hasValue(7);
        assertThat(storedKeys(user)).containsExactly(keyOf(user, "avatar", imageId));
    }

    @Test
    void committingBeforeTheFileArrivedCanBeRetriedOnceItHas() throws Exception {
        long user = newUser();
        HttpResponse<String> ticket = requestTicket(user, "avatar", "image/png", 33);
        String imageId = string(ticket.body(), "imageId");

        HttpResponse<String> early = commit(user, "avatar", imageId);

        assertThat(early.statusCode()).isEqualTo(409);
        assertThat(early.body()).contains("\"code\":\"upload_incomplete\"");

        STORAGE.putViaUrl(string(ticket.body(), "uploadUrl"), ImageFixtures.png(512, 512), "image/png");
        assertThat(commit(user, "avatar", imageId).statusCode()).isEqualTo(200);
    }

    @Test
    void askingForTheSameSlotAgainWhileTheTicketIsUsableGivesTheSameImageId() throws Exception {
        long user = newUser();

        String first = string(requestTicket(user, "avatar", "image/png", 100).body(), "imageId");
        String second = string(requestTicket(user, "avatar", "image/jpeg", 200).body(), "imageId");

        assertThat(second).isEqualTo(first);
        assertThat(ledgerRows(user)).isEqualTo(1);
    }

    // ---- refusals

    @Test
    void aFileThatIsNotAnImageIsRefusedAndDeletedAndNeverShown() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", "<html>definitely not a png</html>".getBytes());

        HttpResponse<String> refused = commit(user, "avatar", imageId);

        assertThat(refused.statusCode()).isEqualTo(422);
        assertThat(refused.body()).contains("\"code\":\"image_rejected\"");
        assertThat(profileOf(user)).contains("\"avatarUrl\":null");
        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
        assertThat(commit(user, "avatar", imageId).statusCode()).isEqualTo(404);
    }

    @Test
    void anImageTooBigInPixelsForTheSlotIsRefused() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(2000, 2000));

        assertThat(commit(user, "avatar", imageId).statusCode()).isEqualTo(422);
    }

    @Test
    void aTicketRequestForAnUnallowedTypeOrSizeIsRefusedAndRecordsNothing() throws Exception {
        long user = newUser();

        assertThat(requestTicket(user, "avatar", "image/gif", 100).statusCode()).isEqualTo(422);
        assertThat(requestTicket(user, "avatar", "image/png", 50_000_000).statusCode()).isEqualTo(422);
        assertThat(requestTicket(user, "avatar", "image/png", 0).statusCode()).isEqualTo(400);

        assertThat(ledgerRows(user)).isZero();
    }

    @Test
    void anotherUsersTicketCannotBeCommittedAndTheirFileIsUntouched() throws Exception {
        long alice = newUser();
        long mallory = newUser();
        String imageId = upload(alice, "avatar", "image/png", ImageFixtures.png(512, 512));

        assertThat(commit(mallory, "avatar", imageId).statusCode()).isEqualTo(404);

        assertThat(storedKeys(alice)).containsExactly(keyOf(alice, "avatar", imageId));
        assertThat(commit(alice, "avatar", imageId).statusCode()).isEqualTo(200);
        assertThat(profileOf(mallory)).contains("\"avatarUrl\":null");
    }

    @Test
    void aTicketForOneSlotCannotBeCommittedIntoTheOther() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(512, 512));

        assertThat(commit(user, "banner", imageId).statusCode()).isEqualTo(404);
    }

    @Test
    void unknownAndMalformedImageIdsAreNotFound() throws Exception {
        long user = newUser();

        assertThat(commit(user, "avatar", "0197aaab-0000-7000-8000-00000000ffff").statusCode()).isEqualTo(404);
        assertThat(commit(user, "avatar", "not-a-uuid").statusCode()).isEqualTo(404);
    }

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        assertThat(send("POST", "/api/v1/profiles/@me/images/avatar/upload", null, "{}").statusCode()).isEqualTo(401);
        assertThat(send("POST", "/api/v1/profiles/@me/images/avatar/commit", null, "{}").statusCode()).isEqualTo(401);
        assertThat(send("DELETE", "/api/v1/profiles/@me/images/avatar", null, null).statusCode()).isEqualTo(401);
    }

    // ---- removal

    @Test
    void removingAnImageEmptiesTheSlotAndDeletesTheFile() throws Exception {
        long user = newUser();
        setImage(user, "avatar", 512);

        assertThat(remove(user, "avatar").statusCode()).isEqualTo(204);

        assertThat(profileOf(user)).contains("\"avatarUrl\":null");
        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
        assertThat(remove(user, "avatar").statusCode()).isEqualTo(204);
    }

    // ---- orphan prevention

    @Test
    void anUploadThatWasNeverCommittedIsSweptOnceItsTicketAndGraceHavePassed() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(512, 512));
        assertThat(storedKeys(user)).hasSize(1);

        // not yet due: the sweeper leaves it alone
        Thread.sleep(1500);
        assertThat(storedKeys(user)).hasSize(1);

        jdbc.sql("UPDATE pending_upload SET delete_after = now() - interval '1 second' WHERE image_id = :id")
                .param("id", java.util.UUID.fromString(imageId))
                .update();

        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).isEmpty();
            assertThat(ledgerRows(user)).isZero();
        });
    }

    @Test
    void aFileThatCannotBeDeletedNowIsDeletedByTheSweeperAsSoonAsStorageIsBack() throws Exception {
        long user = newUser();
        String first = setImage(user, "avatar", 512);
        STORAGE.failingDeletes = true;

        String second = setImage(user, "avatar", 256);

        assertThat(profileOf(user)).contains(addressOf(user, "avatar", second));
        assertThat(storedKeys(user)).contains(keyOf(user, "avatar", first));
        assertThat(ledgerRows(user)).isEqualTo(1);

        STORAGE.failingDeletes = false;

        await().atMost(PATIENCE).untilAsserted(() -> {
            assertThat(storedKeys(user)).containsExactly(keyOf(user, "avatar", second));
            assertThat(ledgerRows(user)).isZero();
        });
    }

    @Test
    void aStorageOutageIsServiceUnavailableAndChangesNothing() throws Exception {
        long user = newUser();
        String imageId = upload(user, "avatar", "image/png", ImageFixtures.png(512, 512));
        STORAGE.failing = true;

        HttpResponse<String> response = commit(user, "avatar", imageId);

        assertThat(response.statusCode()).isEqualTo(503);
        STORAGE.failing = false;
        assertThat(profileOf(user)).contains("\"avatarUrl\":null");
        assertThat(commit(user, "avatar", imageId).statusCode()).isEqualTo(200);
    }
}
