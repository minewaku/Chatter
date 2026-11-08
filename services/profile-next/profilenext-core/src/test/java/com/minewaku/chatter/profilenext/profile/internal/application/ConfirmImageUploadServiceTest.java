package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase.Outcome;
import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileDataUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ConfirmImageUploadService;
import com.minewaku.chatter.profilenext.profile.internal.application.command.DeleteProfileDataService;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePromotionQueue;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageDimensions;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfirmImageUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final long AVATAR_BYTES = 5L * 1024 * 1024;
    private static final long BANNER_BYTES = 10L * 1024 * 1024;

    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakePromotionQueue promotions = new FakePromotionQueue();
    private final FakeProfileStore profiles = new FakeProfileStore(promotions);
    private final FakeRemovalQueue removals = new FakeRemovalQueue();
    private final ImagePolicy policy = new ImagePolicy(
            AVATAR_BYTES,
            BANNER_BYTES,
            1024,
            2048,
            Set.of("jpg", "png", "webp"),
            Duration.ofMinutes(15),
            Duration.ofHours(2));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ConfirmImageUploadService service =
            new ConfirmImageUploadService(storage, profiles, removals, promotions, policy, clock);

    private static OwnedImage image(long user, ImageSlot slot, int n) {
        return new OwnedImage(UserId.of(user), slot, ImageIds.at(n));
    }

    private static ConfirmedUpload upload(long user, ImageSlot slot, int n) {
        return upload(user, slot, n, "png", 1000, 100, 100);
    }

    private static ConfirmedUpload upload(long user, ImageSlot slot, int n, String format, long bytes, int w, int h) {
        return new ConfirmedUpload(
                UserId.of(user), slot, ImageIds.at(n), format, bytes, new ImageDimensions(w, h));
    }

    /** A user with a saved profile (the row exists) and, optionally, an avatar already stored. */
    private void profileWithAvatar(long user, Integer avatarId) {
        profiles.applyEdit(UserId.of(user), DisplayName.of("User " + user), null, NOW);
        if (avatarId != null) {
            profiles.setImage(UserId.of(user), ImageSlot.AVATAR, ImageIds.at(avatarId), NOW);
            promotions.enqueue(image(user, ImageSlot.AVATAR, avatarId), NOW);
        }
        profiles.calls.clear();
    }

    private ConfirmImageUploadUseCase.Result confirm(ConfirmedUpload upload) {
        storage.verifyResult = Optional.of(upload);
        return service.handle(new ConfirmImageUploadUseCase.Command(new byte[] {1}, "sig", "1"));
    }

    @Test
    void aNotificationThatFailsVerificationIsRejectedAndTouchesNothing() {
        storage.verifyResult = Optional.empty();

        assertThatThrownBy(() -> service.handle(new ConfirmImageUploadUseCase.Command(new byte[0], "x", "1")))
                .isInstanceOfSatisfying(
                        UploadConfirmationRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.INVALID_SIGNATURE));
        assertThat(profiles.calls).isEmpty();
        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
        assertThat(promotions.rows).isEmpty();
    }

    @Test
    void aFormatOutsideTheAllowedListIsNotAllowedAndWritesNothing() {
        profileWithAvatar(7, null);

        var result = confirm(upload(7, ImageSlot.AVATAR, 5, "gif", 1000, 100, 100));

        assertThat(result.outcome()).isEqualTo(Outcome.FILE_NOT_ALLOWED);
        assertThat(profiles.calls).isEmpty();
        assertThat(removals.rows).isEmpty();
        assertThat(promotions.rows).isEmpty();
        assertThat(profiles.rows.get(7L).avatar()).isNull();
    }

    @Test
    void aFileOverTheByteOrDimensionLimitIsNotAllowedAndOneAtTheLimitIsNot() {
        profileWithAvatar(7, null);

        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5, "png", AVATAR_BYTES + 1, 100, 100)).outcome())
                .isEqualTo(Outcome.FILE_NOT_ALLOWED);
        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5, "png", 1000, 1025, 100)).outcome())
                .isEqualTo(Outcome.FILE_NOT_ALLOWED);
        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5, "png", 1000, 100, 1025)).outcome())
                .isEqualTo(Outcome.FILE_NOT_ALLOWED);
        assertThat(profiles.calls).isEmpty();

        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5, "png", AVATAR_BYTES, 1024, 1024)).outcome())
                .isEqualTo(Outcome.APPLIED);
    }

    @Test
    void anUploadForAUserWithNoProfileIsIgnoredAndNoRowIsCreated() {
        var result = confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(profiles.calls).containsExactly("findForUpdate:7");
        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
        assertThat(promotions.rows).isEmpty();
    }

    @Test
    void anEmptySlotTakesTheImageAndQueuesItForPromotion() {
        profileWithAvatar(7, null);

        var result = confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(ImageIds.at(5));
        assertThat(promotions.rows).containsOnlyKeys(ImageIds.at(5));
        assertThat(promotions.rows.get(ImageIds.at(5)).image).isEqualTo(image(7, ImageSlot.AVATAR, 5));
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void aNewerImageReplacesTheStoredOneQueuesItForRemovalAndCancelsItsPromotion() {
        profileWithAvatar(7, 3);

        var result = confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(ImageIds.at(5));
        assertThat(removals.rows).containsOnlyKeys(ImageIds.at(3));
        assertThat(removals.rows.get(ImageIds.at(3)).image).isEqualTo(image(7, ImageSlot.AVATAR, 3));
        assertThat(promotions.rows).containsOnlyKeys(ImageIds.at(5));
    }

    @Test
    void anOlderImageIsIgnoredAndChangesNothing() {
        profileWithAvatar(7, 5);

        var result = confirm(upload(7, ImageSlot.AVATAR, 3));

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(ImageIds.at(5));
        assertThat(removals.rows).isEmpty();
        assertThat(promotions.rows).containsOnlyKeys(ImageIds.at(5));
    }

    @Test
    void aRepeatOfTheStoredImageIsIgnoredAndChangesNothing() {
        profileWithAvatar(7, 5);

        var result = confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(profiles.calls).containsExactly("findForUpdate:7");
        assertThat(removals.rows).isEmpty();
        assertThat(promotions.rows).hasSize(1);
    }

    @Test
    void replayingTheSameNotificationLeavesOnePromotionAndNoRemoval() {
        profileWithAvatar(7, null);

        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5)).outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5)).outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(confirm(upload(7, ImageSlot.AVATAR, 5)).outcome()).isEqualTo(Outcome.IGNORED);

        assertThat(promotions.rows).hasSize(1);
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void deliveringNotificationsOutOfOrderKeepsTheNewestImage() {
        profileWithAvatar(7, null);

        confirm(upload(7, ImageSlot.AVATAR, 6));
        confirm(upload(7, ImageSlot.AVATAR, 4));

        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(ImageIds.at(6));
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void avatarAndBannerAreUpdatedIndependentlyInEitherOrder() {
        profileWithAvatar(7, null);

        confirm(upload(7, ImageSlot.BANNER, 8));
        confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(ImageIds.at(5));
        assertThat(profiles.rows.get(7L).banner()).isEqualTo(ImageIds.at(8));
        assertThat(promotions.rows).containsOnlyKeys(ImageIds.at(5), ImageIds.at(8));
    }

    @Test
    void theProfileIsLockedBeforeAnythingIsReadOrWritten() {
        profileWithAvatar(7, 3);

        confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(profiles.calls).containsExactly("findForUpdate:7", "setImage:7:AVATAR");
    }

    @Test
    void aConfirmationAfterTheAccountWasDeletedIsIgnoredAndRecreatesNothing() {
        profileWithAvatar(7, 3);
        new DeleteProfileDataService(profiles, removals, promotions, clock)
                .handle(new DeleteProfileDataUseCase.Command("0197-event", 7, NOW));

        var result = confirm(upload(7, ImageSlot.AVATAR, 5));

        assertThat(result.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(profiles.rows).isEmpty();
        assertThat(promotions.rows).isEmpty();
        assertThat(removals.rows).containsOnlyKeys(ImageIds.at(3));
    }
}
