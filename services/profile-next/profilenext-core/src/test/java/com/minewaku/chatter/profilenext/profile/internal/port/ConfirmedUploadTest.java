package com.minewaku.chatter.profilenext.profile.internal.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageDimensions;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ConfirmedUploadTest {

    private static final ImageDimensions SIZE = new ImageDimensions(640, 480);

    @Test
    void knowsTheImageItIsAbout() {
        ConfirmedUpload upload =
                new ConfirmedUpload(UserId.of(7), ImageSlot.BANNER, ImageIds.at(3), "png", 1234, SIZE);

        assertThat(upload.image()).isEqualTo(new OwnedImage(UserId.of(7), ImageSlot.BANNER, ImageIds.at(3)));
        assertThat(upload.format()).isEqualTo("png");
        assertThat(upload.bytes()).isEqualTo(1234);
        assertThat(upload.dimensions()).isEqualTo(SIZE);
    }

    @Test
    void missingDimensionsAreRejected() {
        assertThatThrownBy(() -> new ConfirmedUpload(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1), "png", 1, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void carriesNoRawPathAndNoAddress() {
        assertThat(Arrays.stream(ConfirmedUpload.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("userId", "slot", "imageId", "format", "bytes", "dimensions");
        assertThat(Arrays.stream(ConfirmedUpload.class.getDeclaredMethods()).map(m -> m.getName()))
                .doesNotContain("publicIdMatchesOwner");
    }
}
