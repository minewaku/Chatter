package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase.Command;
import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase.Outcome;
import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase.Result;
import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadTicketUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.application.command.CommitImageUploadService;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RequestImageUploadTicketService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePendingUploads;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageFixtures;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.Policies;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.RecordingTransactionManager;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.SequentialIds;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.TestClock;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Duration;
import java.util.Optional;
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
        return new Command(7, image.slot().name(), image.imageId().value().toString());
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
        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).contains(image.imageId());
        assertThat(ledger.rows).isEmpty();
        assertThat(storage.objects).containsKey(image);
    }

    @Test
    void replacingAnImageDeletesTheOldKeyAndLeavesNoLedgerRow() {
        OwnedImage first = uploadedPng(ImageSlot.AVATAR, 512);
        commitService().handle(commit(first));
        OwnedImage second = uploadedPng(ImageSlot.AVATAR, 256);

        commitService().handle(commit(second));

        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).contains(second.imageId());
        assertThat(storage.objects).doesNotContainKey(first).containsKey(second);
        assertThat(ledger.rows).isEmpty();
    }

    @Test
    void ifTheOldKeyCannotBeDeletedNowTheLedgerKeepsItForTheSweeperAndTheCommitStillSucceeds() {
        OwnedImage first = uploadedPng(ImageSlot.AVATAR, 512);
        commitService().handle(commit(first));
        OwnedImage second = uploadedPng(ImageSlot.AVATAR, 256);
        storage.failDeletes = true;

        Result result = commitService().handle(commit(second));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).contains(second.imageId());
        assertThat(storage.objects).containsKey(first);
        PendingUpload retired = ledger.rows.get(first.imageId());
        assertThat(retired.state()).isEqualTo(PendingUpload.State.DISCARD);
        assertThat(retired.isDueAt(clock.instant())).isTrue();
    }

    @Test
    void theTwoSlotsAreIndependent() {
        OwnedImage avatar = uploadedPng(ImageSlot.AVATAR, 512);
        OwnedImage banner = uploadedPng(ImageSlot.BANNER, 1500);

        commitService().handle(commit(avatar));
        commitService().handle(commit(banner));

        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).contains(avatar.imageId());
        assertThat(profiles.currentImage(USER, ImageSlot.BANNER)).contains(banner.imageId());
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

    // ---- tickets that cannot be committed

    @Test
    void aTicketThatWasNeverIssuedOrDoesNotParseIsNotFound() {
        assertRefused(new Command(7, "AVATAR", ImageIds.at(55).value().toString()), Reason.UPLOAD_NOT_FOUND);
        assertRefused(new Command(7, "AVATAR", "not-a-uuid"), Reason.UPLOAD_NOT_FOUND);
        assertRefused(new Command(7, "AVATAR", null), Reason.UPLOAD_NOT_FOUND);
    }

    @Test
    void anotherUsersTicketOrAnotherSlotIsNotFoundAndNothingIsDeleted() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);

        assertRefused(new Command(8, "AVATAR", image.imageId().value().toString()), Reason.UPLOAD_NOT_FOUND);
        assertRefused(new Command(7, "BANNER", image.imageId().value().toString()), Reason.UPLOAD_NOT_FOUND);

        assertThat(storage.objects).containsKey(image);
        assertThat(ledger.rows.get(image.imageId()).isPending()).isTrue();
    }

    @Test
    void anExpiredTicketCannotBeCommittedAndItsFileIsLeftToTheSweeper() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        clock.advance(Duration.ofMinutes(5));

        assertRefused(commit(image), Reason.UPLOAD_EXPIRED);

        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).isEmpty();
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
    void aFileTooLargeForTheSlotIsRefusedAndDeleted() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, new byte[5_000_001], "image/png");

        assertRefused(commit(image), Reason.FILE_TOO_LARGE);

        assertThat(storage.objects).doesNotContainKey(image);
        assertThat(ledger.rows).isEmpty();
        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).isEmpty();
    }

    @Test
    void bytesThatAreNotAnImageAreRefusedEvenWhenTheyClaimToBeOne() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, "<html>not an image</html>".getBytes(), "image/png");

        assertRefused(commit(image), Reason.UNREADABLE_IMAGE);

        assertThat(storage.objects).doesNotContainKey(image);
    }

    @Test
    void aFileWhoseBytesDisagreeWithTheRecordedTypeIsRefused() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, ImageFixtures.png(512, 512), "image/jpeg");

        assertRefused(commit(image), Reason.CONTENT_TYPE_MISMATCH);
    }

    @Test
    void anImageLargerInPixelsThanTheSlotAllowsIsRefused() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 1025);

        assertRefused(commit(image), Reason.DIMENSIONS_NOT_ALLOWED);
    }

    @Test
    void aRefusalRecordsItsIntentBeforeDeletingSoAFailedDeleteIsStillCleanedUpLater() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, "garbage".getBytes(), "image/png");
        storage.failDeletes = true;

        assertRefused(commit(image), Reason.UNREADABLE_IMAGE);

        PendingUpload row = ledger.rows.get(image.imageId());
        assertThat(row.state()).isEqualTo(PendingUpload.State.DISCARD);
        assertThat(row.isDueAt(clock.instant())).isTrue();
        assertThat(storage.objects).containsKey(image);
    }

    @Test
    void aRefusedTicketCannotBeCommittedAgain() {
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, "garbage".getBytes(), "image/png");
        assertRefused(commit(image), Reason.UNREADABLE_IMAGE);

        assertRefused(commit(image), Reason.UPLOAD_NOT_FOUND);
    }

    // ---- the provider validates for us

    @Test
    void aProviderThatValidatesOnUploadSkipsTheHeaderRead() {
        storage.validatesOnUpload = true;
        OwnedImage image = ticket(ImageSlot.AVATAR);
        storage.upload(image, "provider already checked this".getBytes(), "image/png");

        Result result = commitService().handle(commit(image));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(storage.calls).noneMatch(call -> call.startsWith("header:"));
    }

    @Test
    void otherwiseTheHeaderIsReadAfterTheStat() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        storage.calls.clear();

        commitService().handle(commit(image));

        assertThat(storage.calls.get(0)).startsWith("stat:");
        assertThat(storage.calls.get(1)).startsWith("header:");
    }

    // ---- failures change nothing

    @Test
    void aStorageOutageIsNotAVerdictAndChangesNothing() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        storage.failStat = true;

        assertThatThrownBy(() -> commitService().handle(commit(image)))
                .isInstanceOf(ImageStorageUnavailableException.class);

        assertThat(storage.objects).containsKey(image);
        assertThat(ledger.rows.get(image.imageId()).isPending()).isTrue();
        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).isEmpty();
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

        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).isEmpty();
        assertThat(transactions.events).endsWith("rollback");
    }

    @Test
    void aCommitThatLosesARaceAgainstTheSameCommitIsAnAlreadyAppliedNotAnError() {
        OwnedImage image = uploadedPng(ImageSlot.AVATAR, 512);
        ledger = new FakePendingUploads() {
            @Override
            public Optional<PendingUpload> lockPending(UserId userId, ImageId imageId) {
                // the winner commits between our checks and our lock
                profiles.replaceImage(userId, ImageSlot.AVATAR, imageId, clock.instant());
                rows.remove(imageId);
                return Optional.empty();
            }
        };
        ledger.rows.put(image.imageId(), PendingUpload.issue(image, clock.instant(), Duration.ofMinutes(5), Duration.ofMinutes(10)));

        Result result = commitService().handle(commit(image));

        assertThat(result.outcome()).isEqualTo(Outcome.ALREADY_APPLIED);
        assertThat(profiles.currentImage(USER, ImageSlot.AVATAR)).contains(image.imageId());
    }

    @Test
    void noStorageCallRunsInsideATransaction() {
        OwnedImage first = uploadedPng(ImageSlot.AVATAR, 512);
        commitService().handle(commit(first));
        OwnedImage second = uploadedPng(ImageSlot.AVATAR, 256);
        transactions.events.clear();
        FakeImageStorage watching = new FakeImageStorage() {
            @Override
            public Optional<StoredObject> stat(OwnedImage image) {
                assertThat(transactions.events).isEmpty();
                return storage.stat(image);
            }

            @Override
            public byte[] readHeader(OwnedImage image, int maxBytes) {
                assertThat(transactions.events).isEmpty();
                return storage.readHeader(image, maxBytes);
            }

            @Override
            public void delete(OwnedImage image) {
                assertThat(transactions.events).endsWith("commit");
                storage.delete(image);
            }
        };

        new CommitImageUploadService(watching, profiles, ledger, Policies.standard(), clock, transactions.template())
                .handle(commit(second));

        assertThat(transactions.events).containsExactly("begin", "commit");
    }
}
