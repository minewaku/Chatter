package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PemKeysTest {

    @TempDir
    Path dir;

    @Test
    void loadsKeyPairFromPemText() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        var props = props(RsaKeyFixtures.privatePem(pair), RsaKeyFixtures.publicPem(pair));

        var keys = PemKeys.load(props);

        assertThat(keys.publicKey().getEncoded()).isEqualTo(pair.getPublic().getEncoded());
        assertThat(keys.privateKey().getEncoded()).isEqualTo(pair.getPrivate().getEncoded());
    }

    @Test
    void loadsKeyPairFromFileLocation() throws Exception {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        Path priv = Files.writeString(dir.resolve("priv.pem"), RsaKeyFixtures.privatePem(pair), StandardCharsets.UTF_8);
        Path pub = Files.writeString(dir.resolve("pub.pem"), RsaKeyFixtures.publicPem(pair), StandardCharsets.UTF_8);

        var keys = PemKeys.load(props("file:" + priv, "file:" + pub));

        assertThat(keys.publicKey().getEncoded()).isEqualTo(pair.getPublic().getEncoded());
    }

    @Test
    void blankPrivateKeyNamesThePropertyAndLeaksNothing() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        assertThatThrownBy(() -> PemKeys.load(props(" ", RsaKeyFixtures.publicPem(pair))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity-access.security.jwt.private-key");
    }

    @Test
    void malformedPublicKeyNamesThePropertyAndLeaksNothing() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        String garbage = "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----";
        assertThatThrownBy(() -> PemKeys.load(props(RsaKeyFixtures.privatePem(pair), garbage)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity-access.security.jwt.public-key")
                .hasMessageNotContaining("AAAA");
    }

    @Test
    void mismatchedKeyPairIsRejected() {
        KeyPair pair = RsaKeyFixtures.newKeyPair();
        KeyPair other = RsaKeyFixtures.newKeyPair();
        assertThatThrownBy(() -> PemKeys.load(props(RsaKeyFixtures.privatePem(pair), RsaKeyFixtures.publicPem(other))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("do not form a matching key pair");
    }

    private static JwtProperties props(String priv, String pub) {
        return new JwtProperties(priv, pub, "identity-access", "chatter", Duration.ofMinutes(15));
    }
}
