package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.profile.api.exception.InvalidProfileValueException;
import org.junit.jupiter.api.Test;

class DisplayNameTest {

    private static final String FAMILY = "👨‍👩‍👧‍👦";

    private static void assertRefused(String raw, String rule) {
        assertThatThrownBy(() -> DisplayName.of(raw))
                .isInstanceOfSatisfying(InvalidProfileValueException.class, e -> {
                    assertThat(e.rule()).isEqualTo(rule);
                    assertThat(e.field()).isEqualTo("displayName");
                });
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(DisplayName.of("  Mine  ").value()).isEqualTo("Mine");
    }

    @Test
    void acceptsExactly32AndRefuses33() {
        assertThat(DisplayName.of("a".repeat(32)).value()).hasSize(32);
        assertRefused("a".repeat(33), "display_name_length");
    }

    @Test
    void refusesEmptySpacesOnlyAndTabOnlyAsBlank() {
        assertRefused("", "display_name_blank");
        assertRefused("     ", "display_name_blank");
        assertRefused("\t", "display_name_blank");
    }

    @Test
    void refusesControlCharacters() {
        assertRefused("a\u0000b", "display_name_control_characters");
        assertRefused("a\u001Fb", "display_name_control_characters");
        assertRefused("a\u007Fb", "display_name_control_characters");
        assertRefused("a\nb", "display_name_control_characters");
        assertRefused("a​b", "display_name_control_characters");
    }

    @Test
    void anEmojiSequenceCountsAsOneCharacter() {
        assertThat(DisplayName.of(FAMILY).value()).isEqualTo(FAMILY);
        assertThat(DisplayName.of(FAMILY.repeat(32)).value()).isEqualTo(FAMILY.repeat(32));
        assertRefused(FAMILY.repeat(33), "display_name_length");
    }

    @Test
    void anAccentedLetterBuiltFromCombiningMarksCountsAsOne() {
        String decomposed = "é".repeat(32);

        assertThat(DisplayName.of(decomposed).value()).isEqualTo(decomposed);
    }

    @Test
    void rawInputOver1024UnitsIsRefusedBeforeCounting() {
        assertRefused("a".repeat(1025), "display_name_length");
    }

    @Test
    void doesNotChangeCase() {
        assertThat(DisplayName.of("MiNe").value()).isEqualTo("MiNe");
    }

    @Test
    void theMessageStatesTheRealLimitsAndNeverTheSubmittedValue() {
        assertThatThrownBy(() -> DisplayName.of("x".repeat(40)))
                .hasMessage("Display name must be 1 to 32 characters.")
                .hasMessageNotContaining("xxx");
    }
}
