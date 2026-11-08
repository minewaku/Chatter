package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageDimensions;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImagePolicyTest {

    private static final long AVATAR_BYTES = 5L * 1024 * 1024;
    private static final long BANNER_BYTES = 10L * 1024 * 1024;

    private final ImagePolicy policy = new ImagePolicy(
            AVATAR_BYTES, BANNER_BYTES, 1024, 2048, Set.of("jpg", "png", "webp"), Duration.ofMinutes(15), Duration.ofHours(2));

    private static ConfirmedUpload upload(ImageSlot slot, String format, long bytes, int width, int height) {
        return new ConfirmedUpload(
                UserId.of(7), slot, ImageIds.at(1), format, bytes, new ImageDimensions(width, height));
    }

    @Test
    void aFileExactlyAtTheLimitsPasses() {
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "png", AVATAR_BYTES, 1024, 1024))).isTrue();
        assertThat(policy.permits(upload(ImageSlot.BANNER, "png", BANNER_BYTES, 2048, 2048))).isTrue();
    }

    @Test
    void oneByteOrOnePixelOverFails() {
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "png", AVATAR_BYTES + 1, 1024, 1024))).isFalse();
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "png", AVATAR_BYTES, 1025, 1024))).isFalse();
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "png", AVATAR_BYTES, 1024, 1025))).isFalse();
        assertThat(policy.permits(upload(ImageSlot.BANNER, "png", BANNER_BYTES, 2049, 2048))).isFalse();
    }

    @Test
    void theFormatIsComparedWithoutRegardToCase() {
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "PNG", 1000, 100, 100))).isTrue();
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "gif", 1000, 100, 100))).isFalse();
    }

    @Test
    void theSlotsHaveIndependentLimits() {
        // Fine for a banner, too big for an avatar.
        assertThat(policy.permits(upload(ImageSlot.BANNER, "png", AVATAR_BYTES + 1, 2000, 2000))).isTrue();
        assertThat(policy.permits(upload(ImageSlot.AVATAR, "png", AVATAR_BYTES + 1, 2000, 2000))).isFalse();
        assertThat(policy.maxDimension(ImageSlot.AVATAR)).isEqualTo(1024);
        assertThat(policy.maxDimension(ImageSlot.BANNER)).isEqualTo(2048);
    }

    @Test
    void aNullFormatIsNotAllowed() {
        assertThat(policy.allows(null)).isFalse();
    }
}
