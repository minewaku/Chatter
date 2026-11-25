package com.minewaku.chatter.profile.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.profile.api.command.CommitImageUploadUseCase.Command;
import com.minewaku.chatter.profile.profile.api.command.CommitImageUploadUseCase.Outcome;
import com.minewaku.chatter.profile.profile.api.command.CommitImageUploadUseCase.Result;
import com.minewaku.chatter.profile.profile.api.command.RequestImageUploadTicketUseCase;
import com.minewaku.chatter.profile.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profile.profile.api.exception.ImageUploadRejectedException;
import com.minewaku.chatter.profile.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profile.profile.internal.application.command.CommitImageUploadService;
import com.minewaku.chatter.profile.profile.internal.application.command.RequestImageUploadTicketService;
import com.minewaku.chatter.profile.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profile.profile.internal.application.fakes.FakePendingUploads;
import com.minewaku.chatter.profile.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profile.profile.internal.application.fakes.ImageFixtures;
import com.minewaku.chatter.profile.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profile.profile.internal.application.fakes.Policies;
import com.minewaku.chatter.profile.profile.internal.application.fakes.RecordingTransactionManager;
import com.minewaku.chatter.profile.profile.internal.application.fakes.SequentialIds;
import com.minewaku.chatter.profile.profile.internal.application.fakes.TestClock;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profile.profile.internal.model.Profile;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import java.time.Duration;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommitImageUploadServiceTest {

    private static final UserId USER = UserId.of(7);

    private final TestClock clock = TestClock.at("2026-11-04T10:00:00Z");
    private final RecordingTransactionManager transactions = new RecordingTransactionManager();
    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakeProfileStore profiles = new FakeProfileStore();
    private FakePendingUploads ledger = new FakePendingUploads();
    private final SequentialIds ids = new SequentialIds();

    private CommitImageUploadService commitService() {
        return new CommitImageUploadService(
                storage, profiles, ledger, Policies.standard(), clock, transactions.template());
    }

    private RequestImageUploadTicketService requestService() {
        return new RequestImageUploadTicketService(
                storage, ledger, Policies.standard(), ids, clock, transactions.template());
    }

    /** Asks for a ticket the way a client would and returns the image it can now upload. */
    private OwnedImage ticket(ImageSlot slot) {
        var result = requestService().handle(new RequestImageUploadTicketUseCase.Command(7, slot.name(), "image/png", 1000));
        return new OwnedImage(USER, slot, ImageId.of(result.imageId()));
    }

    private OwnedImage uploadedPng(ImageSlot slot, int size) {
        OwnedImage image = ticket(slot);
        storage.upload(image, ImageFixtures.png(size, size), "image/png");
        return image;
    }

    private Command commit(OwnedImage image) {
        return new Command(7, image.imageId().value().toString());
    }

    private void assertRefused(Command command, Reason reason) {
        assertThatThrownBy(() -> commitService().handle(command))
                .isInstanceOfSatisfying(
                        ImageUploadRejectedException.class, e -> assertThat(e.reason()).isEqualTo(reason));
    }

    // ---- the happy path

    @Test
    void aCommittedUploadBecomesTheSlotsImageAndLeavesTheLedger() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);

        Result result = commitService().handle(commit(image));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(result.path()).isEqualTo("profiles/7/avatar/" + image.imageId().value());
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isEqualTo(image.imageId());
        assertThat(ledger.rows).isEmpty();
        assertThat(storage.objects).containsKey(image);
    }

    @Test
    void replacingAnImageLeavesTheOldKeyDueInTheLedgerForTheSweeper() {
        OwnedImage first = uploadedPng(ImageSlot.AVATAR, 512);
        commitService().handle(commit(first));
        OwnedImage second = uploadedPng(ImageSlot.AVATAR, 256);

        commitService().handle(commit(second));

        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isEqualTo(second.imageId());
        assertThat(ledger.rows).containsOnlyKeys(first.imageId());
        PendingUpload retired = ledger.rows.get(first.imageId());
        assertThat(retired.isPending()).isFalse();
        assertThat(retired.isDueAt(clock.instant())).isTrue();
        assertThat(storage.calls).noneMatch(call -> call.startsWith("delete:"));
    }

    @Test
    void committingOneTicketRetiresTheOtherTicketsOfTheSlotKeepingTheirSweepTime() {
        OwnedImage chosen = uploadedPng(ImageSlot.AVATAR, 512);
        OwnedImage other = ticket(ImageSlot.AVATAR);
        OwnedImage banner = ticket(ImageSlot.BANNER);
        Instant otherSweep = ledger.rows.get(other.imageId()).deleteAfter();

        commitService().handle(commit(chosen));

        PendingUpload retired = ledger.rows.get(other.imageId());
        assertThat(retired.isPending()).isFalse();
        assertThat(retired.deleteAfter()).isEqualTo(otherSweep);
        assertThat(ledger.rows).doesNotContainKey(chosen.imageId());
        assertThat(ledger.rows.get(banner.imageId()).isPending()).isTrue();
    }

    @Test
    void aTicketRetiredByAnotherCommitCanNoLongerBeCommitted() {
        OwnedImage chosen = uploadedPng(ImageSlot.AVATAR, 512);
        OwnedImage late = uploadedPng(ImageSlot.AVATAR, 256);
        commitService().handle(commit(chosen));

        assertRefused(commit(late), Reason.UPLOAD_NOT_FOUND);
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isEqualTo(chosen.imageId());
    }

    @Test
    void theTwoSlotsAreIndependent() {
        OwnedImage avatar = uploadedPng(ImageSlot.AVATAR, 512);
        OwnedImage banner = uploadedPng(ImageSlot.BANNER, 1500);

        commitService().handle(commit(avatar));
        commitService().handle(commit(banner));

        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isEqualTo(avatar.imageId());
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.BANNER)).isEqualTo(banner.imageId());
    }

    @Test
    void aRepeatedCommitAnswersAlreadyAppliedWithoutTouchingStorage() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        commitService().handle(commit(image));
        storage.calls.clear();

        Result again = commitService().handle(commit(image));

        assertThat(again.outcome()).isEqualTo(Outcome.ALREADY_APPLIED);
        assertThat(storage.calls).isEmpty();
    }

    @Test
    void theResultNamesTheSlotTheTicketWasIssuedForAlsoOnARepeat() {
        OwnedImage banner = uploadedPng(ImageSlot.BANNER, 1500);

        Result first = commitService().handle(commit(banner));
        Result again = commitService().handle(commit(banner));

        assertThat(first.slot()).isEqualTo("BANNER");
        assertThat(again.outcome()).isEqualTo(Outcome.ALREADY_APPLIED);
        assertThat(again.slot()).isEqualTo("BANNER");
        assertThat(again.path()).isEqualTo("profiles/7/banner/" + banner.imageId().value());
    }

    // ---- tickets that cannot be committed

    @Test
    void aTicketThatWasNeverIssuedOrDoesNotParseIsNotFound() {
        assertRefused(new Command(7, ImageIds.at(55).value().toString()), Reason.UPLOAD_NOT_FOUND);
        assertRefused(new Command(7, "not-a-uuid"), Reason.UPLOAD_NOT_FOUND);
        assertRefused(new Command(7, null), Reason.UPLOAD_NOT_FOUND);
    }

    @Test
    void anotherUsersTicketIsNotFoundAndNothingIsDeleted() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);

        assertRefused(new Command(8, image.imageId().value().toString()), Reason.UPLOAD_NOT_FOUND);

        assertThat(storage.objects).containsKey(image);
        assertThat(ledger.rows.get(image.imageId()).isPending()).isTrue();
    }

    @Test
    void anExpiredTicketCannotBeCommittedAndItsFileIsLeftToTheSweeper() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        clock.advance(Duration.ofMinutes(5));

        assertRefused(commit(image), Reason.UPLOAD_EXPIRED);

        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isNull();
        assertThat(storage.objects).containsKey(image);
        assertThat(ledger.rows).containsKey(image.imageId());
    }

    @Test
    void aTicketWhoseFileHasNotArrivedYetCanBeRetried() {
        OwnedImage image = ticket(ImageSlot.AVATAR);

        assertRefused(commit(image), Reason.UPLOAD_INCOMPLETE);
        storage.upload(image, ImageFixtures.png(512, 512), "image/png");
        Result result = commitService().handle(commit(image));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
    }

    // ---- files that are refused

    @Test
    void bytesThatAreNotAnImageAreRefusedEvenWhenTheyClaimToBeOne() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, "<html>not an image</html>".getBytes(), "image/png");

        assertRefused(commit(image), Reason.UNREADABLE_IMAGE);

        PendingUpload row = ledger.rows.get(image.imageId());
        assertThat(row.isPending()).isFalse();
        assertThat(row.isDueAt(clock.instant())).isTrue();
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isNull();
        assertThat(storage.calls).noneMatch(call -> call.startsWith("delete:"));
    }

    @Test
    void anImageLargerInPixelsThanTheSlotAllowsIsRefused() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 1025);

        assertRefused(commit(image), Reason.DIMENSIONS_NOT_ALLOWED);
    }

    @Test
    void aRefusedTicketCannotBeCommittedAgain() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, "garbage".getBytes(), "image/png");
        assertRefused(commit(image), Reason.UNREADABLE_IMAGE);

        assertRefused(commit(image), Reason.UPLOAD_NOT_FOUND);
    }

    // ---- reads

    @Test
    void theFileIsReadOnceAndItsLengthIsNotJudged() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, java.util.Arrays.copyOf(ImageFixtures.png(512, 512), 6_000_000), "image/png"); // 6 MB, over the avatar limit
        storage.calls.clear();

        commitService().handle(commit(image));

        assertThat(storage.calls).hasSize(1);
        assertThat(storage.calls.get(0)).startsWith("read:");
    }

    // ---- failures change nothing

    @Test
    void aStorageOutageIsNotAVerdictAndChangesNothing() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        storage.failRead = true;

        assertThatThrownBy(() -> commitService().handle(commit(image)))
                .isInstanceOf(ImageStorageUnavailableException.class);

        assertThat(storage.objects).containsKey(image);
        assertThat(ledger.rows.get(image.imageId()).isPending()).isTrue();
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isNull();
        assertThat(storage.calls).noneMatch(call -> call.startsWith("delete:"));
    }

    @Test
    void ifTheTicketVanishesBeforeTheCommitPointNothingIsChanged() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        ledger = new FakePendingUploads() {
            @Override
            public Optional<PendingUpload> lockPending(UserId userId, ImageId imageId) {
                return Optional.empty(); // retired by an account deletion in the meantime
            }
        };
        ledger.rows.put(image.imageId(), PendingUpload.issue(image, clock.instant(), Duration.ofMinutes(5), Duration.ofMinutes(10)));

        assertRefused(commit(image), Reason.UPLOAD_NOT_FOUND);

        // the profile write happens first inside the transaction; the rollback is what undoes it in a real database
        assertThat(transactions.events).endsWith("rollback");
    }

    @Test
    void aCommitThatLosesARaceAgainstTheSameCommitIsAnAlreadyAppliedNotAnError() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        ledger = new FakePendingUploads() {
            @Override
            public Optional<PendingUpload> findPending(UserId userId, ImageId imageId) {
                Optional<PendingUpload> found = super.findPending(userId, imageId);
                if (found.isPresent()) {
                    // the winner commits right after we read the ticket
                    profiles.replaceImage(userId, ImageSlot.AVATAR, imageId, clock.instant());
                    rows.remove(imageId);
                }
                return found;
            }
        };
        ledger.rows.put(image.imageId(), PendingUpload.issue(image, clock.instant(), Duration.ofMinutes(5), Duration.ofMinutes(10)));

        Result result = commitService().handle(commit(image));

        assertThat(result.outcome()).isEqualTo(Outcome.ALREADY_APPLIED);
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isEqualTo(image.imageId());
    }

    @Test
    void aCommitWhoseTicketIsGoneBecauseTheWinnerFinishedFirstIsAnAlreadyApplied() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        // the winner finishes between our read of the profile and our read of the ticket: the ticket is gone and the
        // profile now holds the image, although our first read of it did not
        FakeProfileStore winnerFinishedAfterOurFirstRead = new FakeProfileStore() {
            private boolean first = true;

            @Override
            public Profile getOrDefault(UserId userId) {
                if (first) {
                    first = false;
                    return Profile.defaultsFor(userId);
                }
                return super.getOrDefault(userId);
            }
        };
        winnerFinishedAfterOurFirstRead.replaceImage(USER, ImageSlot.AVATAR, image.imageId(), clock.instant());
        ledger.rows.remove(image.imageId());

        Result result = new CommitImageUploadService(
                        storage, winnerFinishedAfterOurFirstRead, ledger, Policies.standard(), clock, transactions.template())
                .handle(commit(image));

        assertThat(result.outcome()).isEqualTo(Outcome.ALREADY_APPLIED);
        assertThat(result.slot()).isEqualTo("AVATAR");
    }

    @Test
    void noStorageCallRunsInsideATransaction() {
        OwnedImage first = uploadedPng(ImageSlot.AVATAR, 512);
        commitService().handle(commit(first));
        OwnedImage second = uploadedPng(ImageSlot.AVATAR, 256);
        transactions.events.clear();
        FakeImageStorage watching = new FakeImageStorage() {
            @Override
            public Optional<byte[]> readFirstBytes(OwnedImage image, int maxBytes) {
                assertThat(transactions.events).isEmpty();
                return storage.readFirstBytes(image, maxBytes);
            }
        };

        new CommitImageUploadService(watching, profiles, ledger, Policies.standard(), clock, transactions.template())
                .handle(commit(second));

        assertThat(transactions.events).containsExactly("begin", "commit");
    }
}
