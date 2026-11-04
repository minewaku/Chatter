package com.minewaku.chatter.profilenext.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    void acceptsAPositiveValue() {
        assertThat(UserId.of(123456789L).value()).isEqualTo(123456789L);
    }

    @Test
    void refusesZero() {
        assertThatThrownBy(() -> UserId.of(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user id must be positive");
    }

    @Test
    void refusesANegativeValue() {
        assertThatThrownBy(() -> UserId.of(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user id must be positive");
    }
}
