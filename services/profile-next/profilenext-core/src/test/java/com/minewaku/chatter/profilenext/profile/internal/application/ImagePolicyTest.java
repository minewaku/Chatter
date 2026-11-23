package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SniffedImage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.Policies;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageDimensions;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import org.junit.jupiter.api.Test;

class ImagePolicyTest {

    private final ImagePolicy policy = Policies.standard();

    @Test
    void mapsContentTypesToFormats() {
        assertThat(ImagePolicy.formatOf("image/jpeg")).contains("jpg");
        assertThat(ImagePolicy.formatOf(" IMAGE/PNG ")).contains("png");
        assertThat(ImagePolicy.formatOf("image/webp")).contains("webp");
        assertThat(ImagePolicy.formatOf("image/gif")).isEmpty();
        assertThat(ImagePolicy.formatOf(null)).isEmpty();
    }

    @Test
    void aRequestForAnAllowedTypeAndSizeIsAccepted() {
        assertThat(policy.checkRequest(ImageSlot.AVATAR, "image/png", 5_000_000)).isEmpty();
        assertThat(policy.checkRequest(ImageSlot.BANNER, "image/webp", 10_000_000)).isEmpty();
    }

    @Test
    void aRequestForAnUnknownTypeOrAnUnallowedFormatIsRefused() {
        assertThat(policy.checkRequest(ImageSlot.AVATAR, "image/gif", 100)).contains(Reason.FORMAT_NOT_ALLOWED);
        assertThat(policy.checkRequest(ImageSlot.AVATAR, "text/html", 100)).contains(Reason.FORMAT_NOT_ALLOWED);
    }

    @Test
    void sizeLimitsDifferBySlot() {
        assertThat(policy.checkRequest(ImageSlot.AVATAR, "image/png", 5_000_001)).contains(Reason.FILE_TOO_LARGE);
        assertThat(policy.checkRequest(ImageSlot.BANNER, "image/png", 5_000_001)).isEmpty();
        assertThat(policy.checkRequest(ImageSlot.BANNER, "image/png", 10_000_001)).contains(Reason.FILE_TOO_LARGE);
        assertThat(policy.checkRequest(ImageSlot.AVATAR, "image/png", 0)).contains(Reason.FILE_TOO_LARGE);
    }

    @Test
    void theFilesOwnBytesMustBeAnAllowedFormatAndFitTheDimensions() {
        SniffedImage realPng = new SniffedImage("png", "image/png", new ImageDimensions(1024, 1024));

        assertThat(policy.checkContent(ImageSlot.AVATAR, realPng)).isEmpty();
        assertThat(policy.checkContent(ImageSlot.AVATAR, new SniffedImage("gif", "image/gif", new ImageDimensions(10, 10))))
                .contains(Reason.FORMAT_NOT_ALLOWED);
        assertThat(policy.checkContent(
                        ImageSlot.AVATAR, new SniffedImage("png", "image/png", new ImageDimensions(1025, 10))))
                .contains(Reason.DIMENSIONS_NOT_ALLOWED);
        assertThat(policy.checkContent(
                        ImageSlot.BANNER, new SniffedImage("png", "image/png", new ImageDimensions(2048, 2048))))
                .isEmpty();
    }
}
