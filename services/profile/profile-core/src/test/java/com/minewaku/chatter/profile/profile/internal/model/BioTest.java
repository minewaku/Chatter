package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.profile.api.exception.InvalidProfileValueException;
import org.junit.jupiter.api.Test;

class BioTest {

    private static final String FAMILY = "👨‍👩‍👧‍👦";

    private static void assertRefused(String raw, String rule) {
        assertThatThrownBy(() -> Bio.of(raw))
                .isInstanceOfSatisfying(InvalidProfileValueException.class, e -> {
                    assertThat(e.rule()).isEqualTo(rule);
                    assertThat(e.field()).isEqualTo("bio");
                });
    }

    @Test
    void mayBeEmpty() {
        assertThat(Bio.of("").value()).isEmpty();
        assertThat(Bio.of("   ").value()).isEmpty();
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(Bio.of("  Hello  ").value()).isEqualTo("Hello");
    }

    @Test
    void acceptsExactly190AndRefuses191() {
        assertThat(Bio.of("a".repeat(190)).value()).hasSize(190);
        assertRefused("a".repeat(191), "bio_length");
    }

    @Test
    void refusesControlCharacters() {
        assertRefused("a\u0000b", "bio_control_characters");
        assertRefused("a\u001Fb", "bio_control_characters");
        assertRefused("a\u007Fb", "bio_control_characters");
        assertRefused("line one\nline two", "bio_control_characters");
    }

    @Test
    void anEmojiSequenceCountsAsOneCharacter() {
        // 90 family emoji are 630 code points but 90 characters; they stay under the 1,024-unit raw cap
        assertThat(Bio.of(FAMILY.repeat(90)).value()).isEqualTo(FAMILY.repeat(90));
        // 191 single emoji are 191 characters, so they are over the limit and still under the raw cap
        assertRefused("\uD83D\uDE00".repeat(191), "bio_length");
        assertThat(Bio.of("\uD83D\uDE00".repeat(190)).value()).hasSize(380);
    }

    @Test
    void rawInputOver1024UnitsIsRefusedBeforeCounting() {
        assertRefused("a".repeat(1025), "bio_length");
    }

    @Test
    void theMessageStatesTheRealLimitsAndNeverTheSubmittedValue() {
        assertThatThrownBy(() -> Bio.of("y".repeat(200)))
                .hasMessage("Bio must be at most 190 characters.")
                .hasMessageNotContaining("yyy");
    }
}
