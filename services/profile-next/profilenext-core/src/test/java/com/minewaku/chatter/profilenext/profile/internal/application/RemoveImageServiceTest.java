package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.RemoveImageUseCase.Command;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RemoveImageService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePendingUploads;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.RecordingTransactionManager;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.TestClock;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import org.junit.jupiter.api.Test;

class RemoveImageServiceTest {

    private static final UserId USER = UserId.of(7);
    private static final OwnedImage AVATAR = new OwnedImage(USER, ImageSlot.AVATAR, ImageIds.at(1));

    private final TestClock clock = TestClock.at("2026-11-04T10:00:00Z");
    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakeProfileStore profiles = new FakeProfileStore();
    private final FakePendingUploads ledger = new FakePendingUploads();

    private RemoveImageService service() {
        return new RemoveImageService(storage, profiles, ledger, clock, new RecordingTransactionManager().template());
    }

    private void holdAvatar() {
        profiles.replaceImage(USER, ImageSlot.AVATAR, AVATAR.imageId(), clock.instant());
        storage.upload(AVATAR, new byte[10], "image/png");
    }

    @Test
    void removingEmptiesTheSlotAndDeletesTheKey() {
        holdAvatar();

        var result = service().handle(new Command(7, "AVATAR"));

        assertThat(result.removed()).isTrue();
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isNull();
        assertThat(storage.objects).isEmpty();
        assertThat(ledger.rows).isEmpty();
    }

    @Test
    void anEmptySlotIsLeftAloneAndTouchesNothing() {
        var result = service().handle(new Command(7, "AVATAR"));

        assertThat(result.removed()).isFalse();
        assertThat(storage.calls).isEmpty();
        assertThat(ledger.rows).isEmpty();
    }

    @Test
    void aFailedDeleteLeavesALedgerRowTheSweeperWillFinish() {
        holdAvatar();
        storage.failDeletes = true;

        var result = service().handle(new Command(7, "AVATAR"));

        assertThat(result.removed()).isTrue();
        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.AVATAR)).isNull();
        PendingUpload row = ledger.rows.get(AVATAR.imageId());
        assertThat(row.isPending()).isFalse();
        assertThat(row.isDueAt(clock.instant())).isTrue();
    }

    @Test
    void theOtherSlotIsNotTouched() {
        holdAvatar();
        profiles.replaceImage(USER, ImageSlot.BANNER, ImageIds.at(2), clock.instant());

        service().handle(new Command(7, "AVATAR"));

        assertThat(profiles.getOrDefault(USER).imageIn(ImageSlot.BANNER)).isEqualTo(ImageIds.at(2));
    }
}
