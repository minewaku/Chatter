package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.SweepPendingUploadsUseCase.Command;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPendingUploadsService;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePendingUploads;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.RecordingTransactionManager;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.TestClock;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SweepPendingUploadsServiceTest {

    private static final UserId USER = UserId.of(7);

    private final TestClock clock = TestClock.at("2026-11-04T10:00:00Z");
    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakePendingUploads ledger = new FakePendingUploads();
    private final RecordingTransactionManager transactions = new RecordingTransactionManager();

    private SweepPendingUploadsService service(int batchSize) {
        return new SweepPendingUploadsService(
                storage, ledger, new SweepPolicy(batchSize, Duration.ofMinutes(5)), clock, transactions.template());
    }

    private OwnedImage image(int n, ImageSlot slot) {
        return new OwnedImage(USER, slot, ImageIds.at(n));
    }

    private void discard(int n, Instant deleteAfter) {
        OwnedImage image = image(n, ImageSlot.AVATAR);
        storage.upload(image, new byte[10], "image/png");
        ledger.discard(image, deleteAfter);
    }

    @Test
    void deletesDueKeysAndTheirRowsAndLeavesTheOthers() {
        discard(1, clock.instant().minusSeconds(60));
        discard(2, clock.instant());
        discard(3, clock.instant().plusSeconds(60));

        var result = service(10).handle(new Command());

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(ledger.rows.keySet()).containsExactly(ImageIds.at(3));
        assertThat(storage.objects.keySet()).containsExactly(image(3, ImageSlot.AVATAR));
    }

    @Test
    void anAbandonedUploadIsDeletedOnlyAfterItsTicketAndGraceHavePassed() {
        OwnedImage image = image(1, ImageSlot.BANNER);
        storage.upload(image, new byte[10], "image/png");
        ledger.insertPending(PendingUpload.issue(image, clock.instant(), Duration.ofMinutes(5), Duration.ofMinutes(10)));

        service(10).handle(new Command());
        assertThat(storage.objects).containsKey(image);

        clock.advance(Duration.ofMinutes(15));
        var result = service(10).handle(new Command());

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(storage.objects).isEmpty();
        assertThat(ledger.rows).isEmpty();
    }

    @Test
    void aKeyThatIsAlreadyGoneStillClearsItsRow() {
        ledger.discard(image(1, ImageSlot.AVATAR), clock.instant());

        var result = service(10).handle(new Command());

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(ledger.rows).isEmpty();
    }

    @Test
    void aFailingDeleteIsPostponedAndDoesNotHoldUpTheRowsBehindIt() {
        OwnedImage stubborn = image(1, ImageSlot.AVATAR);
        storage.upload(stubborn, new byte[10], "image/png");
        ledger.discard(stubborn, clock.instant().minusSeconds(120));
        discard(2, clock.instant().minusSeconds(60));
        FakeImageStorage flaky = new FakeImageStorage() {
            @Override
            public void delete(OwnedImage image) {
                if (image.equals(stubborn)) {
                    throw new com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException("down");
                }
                storage.delete(image);
            }
        };

        var result = new SweepPendingUploadsService(
                        flaky,
                        ledger,
                        new SweepPolicy(10, Duration.ofMinutes(5)),
                        clock,
                        transactions.template())
                .handle(new Command());

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(ledger.rows.keySet()).containsExactly(ImageIds.at(1));
        assertThat(ledger.rows.get(ImageIds.at(1)).deleteAfter()).isEqualTo(clock.instant().plus(Duration.ofMinutes(5)));
        assertThat(storage.objects).containsKey(stubborn);
    }

    @Test
    void aPostponedKeyIsRetriedOnceItsNewTimeComes() {
        OwnedImage stubborn = image(1, ImageSlot.AVATAR);
        storage.upload(stubborn, new byte[10], "image/png");
        ledger.discard(stubborn, clock.instant());
        storage.failDeletes = true;
        service(10).handle(new Command());
        storage.failDeletes = false;
        clock.advance(Duration.ofMinutes(5));

        var result = service(10).handle(new Command());

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(ledger.rows).isEmpty();
    }

    @Test
    void aRunHandlesNoMoreThanTheBatchSize() {
        discard(1, clock.instant());
        discard(2, clock.instant());
        discard(3, clock.instant());

        var result = service(2).handle(new Command());

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(ledger.rows).hasSize(1);
    }

    @Test
    void anEmptyLedgerIsANoOp() {
        var result = service(10).handle(new Command());

        assertThat(result.deleted()).isZero();
        assertThat(storage.calls).isEmpty();
    }
}
