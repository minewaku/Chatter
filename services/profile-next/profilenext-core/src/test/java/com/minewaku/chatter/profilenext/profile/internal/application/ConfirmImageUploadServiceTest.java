package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase.Outcome;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ConfirmImageUploadService;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.model.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfirmImageUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final String OLD = "0197aaaa-0000-7000-8000-000000000001";
    private static final String NEW = "0197aaab-0000-7000-8000-000000000001";

    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakeProfileStore profiles = new FakeProfileStore();
    private final FakeRemovalQueue removals = new FakeRemovalQueue();
    private final ImagePolicy policy = new ImagePolicy(
            5L * 1024 * 1024,
            10L * 1024 * 1024,
            Set.of("jpg", "png", "webp"),
            Duration.ofMinutes(15),
            Duration.ofHours(2));
    private final ConfirmImageUploadService service =
            new ConfirmImageUploadService(storage, profiles, removals, policy, Clock.fixed(NOW, ZoneOffset.UTC));

    private static String avatarId(long user, String uploadId) {
        return "chatter/profile/avatars/" + user + "/" + uploadId;
    }

    private static ConfirmedUpload avatar(long user, String uploadId) {
        return new ConfirmedUpload(
                UserId.of(user), ImageSlot.AVATAR, avatarId(user, uploadId), "https://img/" + uploadId, "png", 1000);
    }

    private ConfirmImageUploadUseCase.Result confirm(ConfirmedUpload upload) {
        storage.verifyResult = Optional.of(upload);
        return service.handle(new ConfirmImageUploadUseCase.Command(new byte[] {1}, "sig", "1"));
    }

    @Test
    void aBadSignatureIsRejectedBeforeAnyRepositoryCall() {
        storage.verifyResult = Optional.empty();

        assertThatThrownBy(() -> service.handle(new ConfirmImageUploadUseCase.Command(new byte[0], "x", "1")))
                .isInstanceOfSatisfying(
                        UploadConfirmationRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.INVALID_SIGNATURE));
        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void aPublicIdOwnedBySomeoneElseIsAUserMismatchAndChangesNothing() {
        ConfirmedUpload mismatch = new ConfirmedUpload(
                UserId.of(7), ImageSlot.AVATAR, avatarId(8, NEW), "https://img/x", "png", 1000);

        assertThatThrownBy(() -> confirm(mismatch))
                .isInstanceOfSatisfying(
                        UploadConfirmationRejectedException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.USER_MISMATCH));
        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void aFormatOutsideTheAllowedListIsReturnedAsNotAllowedAndTheFileIsQueued() {
        ConfirmedUpload gif = new ConfirmedUpload(
                UserId.of(7), ImageSlot.AVATAR, avatarId(7, NEW), "https://img/x", "gif", 1000);

        var result = confirm(gif);

        assertThat(result.outcome()).isEqualTo(Outcome.FILE_NOT_ALLOWED);
        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).containsKey(avatarId(7, NEW));
    }

    @Test
    void aFileOverTheSlotLimitIsNotAllowed() {
        ConfirmedUpload big = new ConfirmedUpload(
                UserId.of(7), ImageSlot.AVATAR, avatarId(7, NEW), "https://img/x", "png", 5L * 1024 * 1024 + 1);
        ConfirmedUpload bigBanner = new ConfirmedUpload(
                UserId.of(7),
                ImageSlot.BANNER,
                "chatter/profile/banners/7/" + NEW,
                "https://img/b",
                "png",
                5L * 1024 * 1024 + 1);

        assertThat(confirm(big).outcome()).isEqualTo(Outcome.FILE_NOT_ALLOWED);
        // the same size is fine for a banner, whose limit is 10 MB
        assertThat(confirm(bigBanner).outcome()).isEqualTo(Outcome.APPLIED);
    }

    @Test
    void aValidConfirmationReplacesTheSlotAndQueuesThePreviousFile() {
        confirm(avatar(7, OLD));

        var result = confirm(avatar(7, NEW));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(profiles.rows.get(7L).avatar().publicId()).isEqualTo(avatarId(7, NEW));
        assertThat(removals.rows).containsOnlyKeys(avatarId(7, OLD));
    }

    @Test
    void aProfileWithNoRowIsCreatedWithTheImage() {
        var result = confirm(avatar(9, NEW));

        assertThat(result.outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(profiles.rows.get(9L).avatar().url()).isEqualTo("https://img/" + NEW);
        assertThat(profiles.rows.get(9L).displayName()).isEmpty();
    }

    @Test
    void queuesNothingWhenSlotWasEmpty() {
        confirm(avatar(7, NEW));

        assertThat(removals.rows).isEmpty();
    }

    @Test
    void theSameConfirmationTwiceChangesNothingTheSecondTime() {
        confirm(avatar(7, NEW));

        var second = confirm(avatar(7, NEW));

        assertThat(second.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(profiles.rows.get(7L).avatar().publicId()).isEqualTo(avatarId(7, NEW));
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void anOlderUploadArrivingAfterANewerOneChangesNothingAndItsFileIsQueued() {
        confirm(avatar(7, NEW));

        var late = confirm(avatar(7, OLD));

        assertThat(late.outcome()).isEqualTo(Outcome.IGNORED);
        assertThat(profiles.rows.get(7L).avatar().publicId()).isEqualTo(avatarId(7, NEW));
        assertThat(removals.rows).containsOnlyKeys(avatarId(7, OLD));
    }
}
