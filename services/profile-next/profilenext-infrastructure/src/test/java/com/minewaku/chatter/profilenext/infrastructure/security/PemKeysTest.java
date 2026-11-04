package com.minewaku.chatter.profilenext.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.infrastructure.security.SigningKeysUnavailableException.Reason;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PemKeysTest {

    private static KeyPair newPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String pem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }

    @Test
    void parsesAValidPublicKey() throws Exception {
        KeyPair pair = newPair();

        var key = PemKeys.parsePublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded()));

        assertThat(key.getEncoded()).isEqualTo(pair.getPublic().getEncoded());
    }

    @Test
    void aMalformedPemNamesTheFieldAndLeaksNothing() {
        String garbage = "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----";

        assertThatThrownBy(() -> PemKeys.parsePublicKey(garbage))
                .isInstanceOfSatisfying(SigningKeysUnavailableException.class, e -> {
                    assertThat(e.reason()).isEqualTo(Reason.KEY_INVALID);
                    assertThat(e).hasNoCause();
                })
                .hasMessageContaining("public-key")
                .hasMessageNotContaining("AAAA");
    }

    @Test
    void aPrivateKeyPassedByMistakeIsRefused() throws Exception {
        KeyPair pair = newPair();
        String privatePem = pem("PRIVATE KEY", pair.getPrivate().getEncoded());

        assertThatThrownBy(() -> PemKeys.parsePublicKey(privatePem))
                .isInstanceOfSatisfying(SigningKeysUnavailableException.class, e -> assertThat(e.reason())
                        .isEqualTo(Reason.KEY_INVALID))
                .hasMessageContaining("public-key");
    }

    @Test
    void aBlankValueIsRefused() {
        assertThatThrownBy(() -> PemKeys.parsePublicKey("  "))
                .isInstanceOf(SigningKeysUnavailableException.class)
                .hasMessageContaining("public-key");
    }
}
