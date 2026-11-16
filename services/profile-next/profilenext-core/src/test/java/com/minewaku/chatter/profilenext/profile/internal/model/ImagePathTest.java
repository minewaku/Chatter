package com.minewaku.chatter.profilenext.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import org.junit.jupiter.api.Test;

class ImagePathTest {

    @Test
    void layoutIsOwnerThenSlotThenImageId() {
        ImageId id = ImageIds.at(3);

        assertThat(ImagePath.of(UserId.of(42), ImageSlot.AVATAR, id)).isEqualTo("profiles/42/avatar/" + id.value());
        assertThat(ImagePath.of(new OwnedImage(UserId.of(42), ImageSlot.BANNER, id)))
                .isEqualTo("profiles/42/banner/" + id.value());
    }
}
