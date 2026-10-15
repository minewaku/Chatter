package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TokenDigestTest {

    @Test
    void producesLowercaseSha256HexOfKnownVector() {
        assertThat(TokenDigest.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void outputIs64LowercaseHexCharacters() {
        assertThat(TokenDigest.sha256Hex("any-token")).matches("[0-9a-f]{64}");
    }

    @Test
    void rejectsBlankInputWithoutEchoingIt() {
        assertThatThrownBy(() -> TokenDigest.sha256Hex("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("   ");
    }
}
