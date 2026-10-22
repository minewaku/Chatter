package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException.Reason;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class PemKeysTest {

    @Test
    void parsesKeyPairFromPemText() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();

        var keys = PemKeys.parse(RsaKeyFixtures.privatePem(pair), RsaKeyFixtures.publicPem(pair));

        assertThat(keys.publicKey().getEncoded()).isEqualTo(pair.getPublic().getEncoded());
        assertThat(keys.privateKey().getEncoded()).isEqualTo(pair.getPrivate().getEncoded());
    }

    @Test
    void toleratesSurroundingWhitespaceAndMissingBeginEndLines() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        String bareBody = RsaKeyFixtures.privatePem(pair)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");

        var keys = PemKeys.parse("  \n" + bareBody + "\n  ", "\n\n" + RsaKeyFixtures.publicPem(pair) + "  ");

        assertThat(keys.publicKey().getEncoded()).isEqualTo(pair.getPublic().getEncoded());
    }

    @Test
    void blankPrivateKeyIsInvalidAndNamesTheField() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();

        assertThatThrownBy(() -> PemKeys.parse(" ", RsaKeyFixtures.publicPem(pair)))
                .isInstanceOfSatisfying(SigningKeysUnavailableException.class, e -> assertThat(e.reason())
                        .isEqualTo(Reason.KEY_INVALID))
                .hasMessageContaining("private-key");
    }

    @Test
    void malformedPublicKeyNamesTheFieldAndLeaksNothing() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        String garbage = "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----";

        assertThatThrownBy(() -> PemKeys.parse(RsaKeyFixtures.privatePem(pair), garbage))
                .isInstanceOfSatisfying(SigningKeysUnavailableException.class, e -> {
                    assertThat(e.reason()).isEqualTo(Reason.KEY_INVALID);
                    assertThat(e).hasNoCause();
                })
                .hasMessageContaining("public-key")
                .hasMessageNotContaining("AAAA");
    }

    @Test
    void notBase64IsInvalidAndLeaksNothing() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();

        assertThatThrownBy(() -> PemKeys.parse("!!!not-base64!!!", RsaKeyFixtures.publicPem(pair)))
                .isInstanceOfSatisfying(SigningKeysUnavailableException.class, e -> assertThat(e.reason())
                        .isEqualTo(Reason.KEY_INVALID))
                .hasMessageNotContaining("not-base64");
    }

    @Test
    void mismatchedKeyPairIsRejected() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        KeyPair other = RsaKeyFixtures.newKeyPair();

        assertThatThrownBy(() -> PemKeys.parse(RsaKeyFixtures.privatePem(pair), RsaKeyFixtures.publicPem(other)))
                .isInstanceOfSatisfying(SigningKeysUnavailableException.class, e -> assertThat(e.reason())
                        .isEqualTo(Reason.KEY_PAIR_MISMATCH))
                .hasMessageContaining("do not form a matching key pair");
    }
}
