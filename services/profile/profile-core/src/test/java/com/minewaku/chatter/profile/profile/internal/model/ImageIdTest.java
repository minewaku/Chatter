package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImageIdTest {

    /** A UUIDv7-shaped value: 48-bit millisecond timestamp, then the version nibble 7. */
    private static ImageId v7(long millis) {
        return new ImageId(new UUID((millis << 16) | 0x7000L, 0x8000_0000_0000_0001L));
    }

    @Test
    void aLaterIdOrdersAfterAnEarlierOne() {
        ImageId earlier = v7(1_790_000_000_000L);
        ImageId later = v7(1_790_000_000_001L);

        assertThat(later).isGreaterThan(earlier);
        assertThat(later.isNewerThan(earlier)).isTrue();
        assertThat(earlier.isNewerThan(later)).isFalse();
    }

    @Test
    void orderFollowsTimeAcrossTheWholeRealisticRange() {
        // The signed comparison of UUID must agree with time: a timestamp near zero and one in 2026.
        assertThat(v7(1_790_000_000_000L).isNewerThan(v7(1L))).isTrue();
        assertThat(v7(1L).isNewerThan(v7(1_790_000_000_000L))).isFalse();
    }

    @Test
    void aRepeatOfTheSameIdIsNotNewer() {
        ImageId id = v7(1_790_000_000_000L);

        assertThat(id.isNewerThan(new ImageId(id.value()))).isFalse();
        assertThat(id).isEqualByComparingTo(new ImageId(id.value()));
    }

    @Test
    void textThatIsNotAUuidIsRejected() {
        assertThatThrownBy(() -> ImageId.of("not-a-uuid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImageId.of("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImageId.of("0-0-0-0-0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImageId.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNullUuidIsRejected() {
        assertThatThrownBy(() -> new ImageId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void theTextFormRoundTrips() {
        ImageId id = v7(1_790_000_000_000L);

        assertThat(ImageId.of(id.value().toString())).isEqualTo(id);
    }
}
