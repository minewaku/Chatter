package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ImageSlotTest {

    @Test
    void hasTheTwoSlots() {
        assertThat(ImageSlot.values()).containsExactly(ImageSlot.AVATAR, ImageSlot.BANNER);
    }

    @Test
    void doesNotKnowAnyStorageFolderName() {
        assertThat(Arrays.stream(ImageSlot.class.getDeclaredMethods()).map(m -> m.getName()))
                .doesNotContain("folder");
    }
}
