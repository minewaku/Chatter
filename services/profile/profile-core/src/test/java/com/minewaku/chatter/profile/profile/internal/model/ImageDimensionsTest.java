package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImageDimensionsTest {

    @Test
    void aSinglePixelIsAccepted() {
        ImageDimensions dimensions = new ImageDimensions(1, 1);

        assertThat(dimensions.width()).isEqualTo(1);
        assertThat(dimensions.height()).isEqualTo(1);
    }

    @Test
    void aZeroOrNegativeSideIsRejected() {
        assertThatThrownBy(() -> new ImageDimensions(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImageDimensions(10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImageDimensions(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImageDimensions(10, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
