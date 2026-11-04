package com.minewaku.chatter.profilenext.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageSlotTest {

    @Test
    void hasTheTwoSlotsWithTheirFolders() {
        assertThat(ImageSlot.values()).containsExactly(ImageSlot.AVATAR, ImageSlot.BANNER);
        assertThat(ImageSlot.AVATAR.folder()).isEqualTo("avatars");
        assertThat(ImageSlot.BANNER.folder()).isEqualTo("banners");
    }
}
