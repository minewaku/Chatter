package com.minewaku.chatter.profile.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.profile.api.command.RequestImageUploadTicketUseCase.Command;
import com.minewaku.chatter.profile.profile.api.command.RequestImageUploadTicketUseCase.Result;
import com.minewaku.chatter.profile.profile.api.exception.ImageUploadRejectedException;
import com.minewaku.chatter.profile.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profile.profile.internal.application.command.RequestImageUploadTicketService;
import com.minewaku.chatter.profile.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profile.profile.internal.application.fakes.FakePendingUploads;
import com.minewaku.chatter.profile.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profile.profile.internal.application.fakes.Policies;
import com.minewaku.chatter.profile.profile.internal.application.fakes.RecordingTransactionManager;
import com.minewaku.chatter.profile.profile.internal.application.fakes.SequentialIds;
import com.minewaku.chatter.profile.profile.internal.application.fakes.TestClock;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RequestImageUploadTicketServiceTest {

    private static final UserId USER = UserId.of(7);

    private final TestClock clock = TestClock.at("2026-11-04T10:00:00Z");
    private final RecordingTransactionManager transactions = new RecordingTransactionManager();
    private FakePendingUploads ledger = new FakePendingUploads();
    private FakeImageStorage storage = new FakeImageStorage();

    private RequestImageUploadTicketService service() {
        return new RequestImageUploadTicketService(
                storage, ledger, Policies.standard(), new SequentialIds(), clock, transactions.template());
    }

    private static Command avatar(String contentType, long size) {
        return new Command(7, "AVATAR", contentType, size);
    }

    @Test
    void issuesATicketForANewImageIdAndRecordsItInTheLedger() {
        Result result = service().handle(avatar("image/png", 1000));

        assertThat(result.imageId()).isEqualTo(ImageIds.at(1).value().toString());
        assertThat(result.method()).isEqualTo("PUT");
        assertThat(result.uploadUrl()).endsWith("profiles/7/avatar/" + ImageIds.at(1).value());
        assertThat(result.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(5)));
        PendingUpload row = ledger.rows.get(ImageIds.at(1));
        assertThat(row.isPending()).isTrue();
        assertThat(row.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(5)));
        assertThat(row.deleteAfter()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
        assertThat(storage.lastValidFor).isEqualTo(Duration.ofMinutes(5));
        assertThat(storage.lastContentType).isEqualTo("image/png");
        assertThat(storage.lastSizeBytes).isEqualTo(1000);
    }

    @Test
    void theLedgerRowIsCommittedBeforeTheTicketIsSigned() {
        storage = new FakeImageStorage() {
            @Override
            public UploadTicket issueUpload(OwnedImage image, String contentType, long sizeBytes, Duration validFor) {
                assertThat(ledger.rows).containsKey(image.imageId());
                assertThat(transactions.events).endsWith("commit");
                return super.issueUpload(image, contentType, sizeBytes, validFor);
            }
        };

        service().handle(avatar("image/png", 1000));

        assertThat(storage.calls).hasSize(1);
    }

    @Test
    void everyRequestGetsItsOwnTicketAndEarlierOnesStayUsable() {
        RequestImageUploadTicketService service = service();

        Result first = service.handle(avatar("image/png", 1000));
        clock.advance(Duration.ofMinutes(2));
        Result second = service.handle(avatar("image/png", 1000));

        assertThat(second.imageId()).isNotEqualTo(first.imageId());
        assertThat(ledger.rows).hasSize(2);
        assertThat(ledger.rows.values()).allSatisfy(row -> assertThat(row.isPending()).isTrue());
        assertThat(second.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(5)));
    }

    @Test
    void eachSlotAndEachUserHasItsOwnTicket() {
        RequestImageUploadTicketService service = service();

        Result avatar = service.handle(avatar("image/png", 1000));
        Result banner = service.handle(new Command(7, "BANNER", "image/png", 1000));
        Result other = service.handle(new Command(8, "AVATAR", "image/png", 1000));

        assertThat(java.util.Set.of(avatar.imageId(), banner.imageId(), other.imageId())).hasSize(3);
    }

    @Test
    void aRefusedRequestWritesAndSignsNothing() {
        assertThatThrownBy(() -> service().handle(avatar("image/gif", 1000)))
                .isInstanceOfSatisfying(
                        ImageUploadRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.FORMAT_NOT_ALLOWED));
        assertThatThrownBy(() -> service().handle(avatar("image/png", 9_000_000)))
                .isInstanceOfSatisfying(
                        ImageUploadRejectedException.class, e -> assertThat(e.reason()).isEqualTo(Reason.FILE_TOO_LARGE));

        assertThat(ledger.rows).isEmpty();
        assertThat(storage.calls).isEmpty();
        assertThat(transactions.events).isEmpty();
    }

    @Test
    void ticketIdsAreRealImageIds() {
        Result result = service().handle(avatar("image/png", 1000));

        assertThat(ImageId.of(result.imageId())).isEqualTo(ImageIds.at(1));
    }
}
