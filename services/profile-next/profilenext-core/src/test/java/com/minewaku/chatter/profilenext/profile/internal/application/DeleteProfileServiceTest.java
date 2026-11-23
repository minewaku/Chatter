package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileUseCase.Command;
import com.minewaku.chatter.profilenext.profile.internal.application.command.DeleteProfileService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePendingUploads;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.TestClock;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeleteProfileServiceTest {

    private static final UserId USER = UserId.of(7);

    private final TestClock clock = TestClock.at("2026-11-04T10:00:00Z");
    private final FakeProfileStore profiles = new FakeProfileStore();
    private final FakePendingUploads ledger = new FakePendingUploads();
    private final DeleteProfileService service = new DeleteProfileService(profiles, ledger, clock);

    private Command notice() {
        return new Command("event-1", 7, clock.instant());
    }

    @Test
    void theRowGoesAndBothImagesAreRetiredForTheSweeper() {
        profiles.replaceImage(USER, ImageSlot.AVATAR, ImageIds.at(1), clock.instant());
        profiles.replaceImage(USER, ImageSlot.BANNER, ImageIds.at(2), clock.instant());

        service.handle(notice());

        assertThat(profiles.rows).isEmpty();
        assertThat(ledger.rows.keySet()).containsExactlyInAnyOrder(ImageIds.at(1), ImageIds.at(2));
        assertThat(ledger.rows.values()).allSatisfy(row -> {
            assertThat(row.isPending()).isFalse();
            assertThat(row.isDueAt(clock.instant())).isTrue();
        });
    }

    @Test
    void pendingTicketsAreRetiredKeepingTheirSweepTimeSoALateUploadIsStillCleanedUp() {
        PendingUpload ticket = PendingUpload.issue(
                new OwnedImage(USER, ImageSlot.AVATAR, ImageIds.at(3)),
                clock.instant(),
                Duration.ofMinutes(5),
                Duration.ofMinutes(10));
        ledger.insert(ticket);

        service.handle(notice());

        PendingUpload retired = ledger.rows.get(ImageIds.at(3));
        assertThat(retired.isPending()).isFalse();
        assertThat(retired.deleteAfter()).isEqualTo(ticket.deleteAfter());
        assertThat(ledger.findPending(USER, ImageIds.at(3))).isEmpty();
    }

    @Test
    void aUserWithNoRowStillGetsTheirTicketsRetired() {
        ledger.insert(PendingUpload.issue(
                new OwnedImage(USER, ImageSlot.BANNER, ImageIds.at(4)),
                clock.instant(),
                Duration.ofMinutes(5),
                Duration.ofMinutes(10)));

        service.handle(notice());

        assertThat(ledger.rows.get(ImageIds.at(4)).isPending()).isFalse();
    }

    @Test
    void otherUsersAreNotTouchedAndARepeatChangesNothing() {
        UserId other = UserId.of(8);
        profiles.replaceImage(other, ImageSlot.AVATAR, ImageIds.at(5), clock.instant());
        profiles.replaceImage(USER, ImageSlot.AVATAR, ImageIds.at(1), clock.instant());

        service.handle(notice());
        service.handle(notice());

        assertThat(profiles.getOrDefault(other).imageIn(ImageSlot.AVATAR)).isEqualTo(ImageIds.at(5));
        assertThat(ledger.rows.keySet()).containsExactly(ImageIds.at(1));
    }
}
