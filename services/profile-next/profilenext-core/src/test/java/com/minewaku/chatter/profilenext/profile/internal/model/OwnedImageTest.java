package com.minewaku.chatter.profilenext.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import org.junit.jupiter.api.Test;

class OwnedImageTest {

    @Test
    void aMissingPartIsRejected() {
        assertThatThrownBy(() -> new OwnedImage(null, ImageSlot.AVATAR, ImageIds.at(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OwnedImage(UserId.of(7), null, ImageIds.at(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OwnedImage(UserId.of(7), ImageSlot.AVATAR, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalPartsGiveEqualRecords() {
        OwnedImage a = new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1));
        OwnedImage b = new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(new OwnedImage(UserId.of(7), ImageSlot.BANNER, ImageIds.at(1)));
    }
}
