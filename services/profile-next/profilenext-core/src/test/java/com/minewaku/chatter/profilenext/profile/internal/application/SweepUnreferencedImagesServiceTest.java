package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.SweepUnreferencedImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepUnreferencedImagesService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredImageInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SweepUnreferencedImagesServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final Instant OLD = NOW.minus(Duration.ofHours(30));
    private static final Instant YOUNG = NOW.minus(Duration.ofHours(2));

    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakeProfileStore profiles = new FakeProfileStore();
    private final FakeRemovalQueue queue = new FakeRemovalQueue();
    private final SweepUnreferencedImagesService service = new SweepUnreferencedImagesService(
            storage, profiles, queue, new SweepPolicy(Duration.ofHours(24)), Clock.fixed(NOW, ZoneOffset.UTC));

    private static OwnedImage avatar(long user, int n) {
        return new OwnedImage(UserId.of(user), ImageSlot.AVATAR, ImageIds.at(n));
    }

    private static StoredImageInfo info(OwnedImage image, Instant createdAt) {
        return new StoredImageInfo(image, createdAt);
    }

    private SweepUnreferencedImagesUseCase.Result run() {
        return service.handle(new SweepUnreferencedImagesUseCase.Command());
    }

    @Test
    void removesOldUnreferencedUnqueuedFilesOnly() {
        profiles.applyEdit(UserId.of(7), null, null, NOW);
        profiles.setImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(2), NOW);
        queue.enqueue(avatar(7, 4), NOW);
        storage.pages.add(FakeImageStorage.page(
                null,
                info(avatar(7, 1), OLD),
                info(avatar(7, 2), OLD),
                info(avatar(7, 3), YOUNG),
                info(avatar(7, 4), OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(avatar(7, 1));
    }

    @Test
    void aFileWhoseQueueRowIsReportedIsRemovedAndTheRowCleared() {
        OwnedImage image = avatar(7, 1);
        queue.enqueue(image, OLD);
        queue.rows.get(image.imageId()).reportedAt = OLD;
        storage.pages.add(FakeImageStorage.page(null, info(image, OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(image);
        assertThat(queue.rows).isEmpty();
    }

    @Test
    void followsTheCursorAcrossPages() {
        storage.pages.add(FakeImageStorage.page("1", info(avatar(7, 1), OLD)));
        storage.pages.add(FakeImageStorage.page(null, info(avatar(7, 2), OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(2);
        assertThat(storage.listCursors).containsExactly(null, "1");
    }

    @Test
    void stopsCleanlyWhenTheProviderReportsARateLimit() {
        storage.pages.add(FakeImageStorage.page("1", info(avatar(7, 1), OLD)));
        storage.pages.add(FakeImageStorage.page(null, info(avatar(7, 2), OLD)));
        storage.listsBeforeRateLimit = 1;

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(avatar(7, 1));
    }

    @Test
    void anImageHeldByTheOwnerInTheOtherSlotIsNotReferencedAndIsRemoved() {
        profiles.applyEdit(UserId.of(7), null, null, NOW);
        profiles.setImage(UserId.of(7), ImageSlot.BANNER, ImageIds.at(1), NOW);
        storage.pages.add(FakeImageStorage.page(null, info(avatar(7, 1), OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
    }

    @Test
    void anImageHeldBySomeoneElsesProfileDoesNotProtectThisOwnersImage() {
        profiles.applyEdit(UserId.of(8), null, null, NOW);
        profiles.setImage(UserId.of(8), ImageSlot.AVATAR, ImageIds.at(1), NOW);
        storage.pages.add(FakeImageStorage.page(null, info(avatar(7, 1), OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(avatar(7, 1));
    }

    @Test
    void anImageWhoseOwnerHasNoProfileIsRemovedOnceOldEnough() {
        storage.pages.add(FakeImageStorage.page(null, info(avatar(99, 1), OLD), info(avatar(99, 2), YOUNG)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(avatar(99, 1));
    }

    @Test
    void aFileThatCannotBeRemovedNowIsSkippedAndTheRestOfThePageIsStillSwept() {
        storage.failingRemovals.add(ImageIds.at(1));
        storage.pages.add(FakeImageStorage.page(null, info(avatar(7, 1), OLD), info(avatar(7, 2), OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(avatar(7, 2));
        assertThat(storage.removeCalls).hasSize(2);
    }

    @Test
    void aFreshStoredImageAtExactlyTheCutoffIsNotOldEnoughToBeConsidered() {
        storage.pages.add(FakeImageStorage.page(null, info(avatar(7, 1), NOW.minus(Duration.ofHours(24)))));

        var result = run();

        assertThat(result.removed()).isZero();
        assertThat(storage.removeCalls).isEmpty();
    }
}
