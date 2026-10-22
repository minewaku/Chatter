package com.minewaku.chatter.identityaccess.start.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException.Reason;
import com.minewaku.chatter.identityaccess.start.support.RsaKeys;
import com.minewaku.chatter.identityaccess.start.support.VaultContainerSupport;
import java.security.KeyPair;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.vault.core.VaultTemplate;

/** The loader against a real Vault: what it returns, and what it says for each way the keys can be unavailable. */
class VaultSigningKeyLoaderTest {

    private static final String CONTAINER_ENDPOINT = "http://vault.test:8200";

    private final String path = "loader-test/" + UUID.randomUUID();
    private final JwtProperties.Keys keys = new JwtProperties.Keys("secret", path);
    private final KeyPair pair = RsaKeys.newKeyPair();

    private VaultSigningKeyLoader loader(VaultTemplate template) {
        return new VaultSigningKeyLoader(template, keys, CONTAINER_ENDPOINT);
    }

    private VaultSigningKeyLoader loader() {
        return loader(VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN));
    }

    private void seed(String privatePem, String publicPem) {
        VaultContainerSupport.put(path, Map.of("private-key", privatePem, "public-key", publicPem));
    }

    private static void assertFailure(Throwable failure, Reason reason, String messagePart) {
        assertThat(failure).isInstanceOf(SigningKeysUnavailableException.class);
        assertThat(((SigningKeysUnavailableException) failure).reason()).isEqualTo(reason);
        assertThat(failure.getMessage()).startsWith("Signing keys").contains(messagePart);
    }

    private static void assertNothingLeaks(Throwable failure, String... secrets) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            String message = String.valueOf(t.getMessage());
            for (String secret : secrets) {
                String body = secret.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
                for (int i = 0; i + 8 <= body.length(); i += 8) {
                    assertThat(message).doesNotContain(body.substring(i, i + 8));
                }
            }
        }
    }

    @Test
    void loadsTheSeededPair() {
        seed(RsaKeys.privatePem(pair), RsaKeys.publicPem(pair));

        var loaded = loader().load();

        assertThat(loaded.privateKey().getEncoded()).isEqualTo(pair.getPrivate().getEncoded());
        assertThat(loaded.publicKey().getEncoded()).isEqualTo(pair.getPublic().getEncoded());
    }

    @Test
    void acceptsKeysWithSurroundingWhitespace() {
        seed("\n  " + RsaKeys.privatePem(pair) + "\n\n", "  \n" + RsaKeys.publicPem(pair) + " ");

        assertThat(loader().load().publicKey().getEncoded())
                .isEqualTo(pair.getPublic().getEncoded());
    }

    @Test
    void honoursAnotherBackendPath() {
        String other = "loader-test/other-" + UUID.randomUUID();
        VaultContainerSupport.put(
                other, Map.of("private-key", RsaKeys.privatePem(pair), "public-key", RsaKeys.publicPem(pair)));
        var loader = new VaultSigningKeyLoader(
                VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN),
                new JwtProperties.Keys("secret", other),
                CONTAINER_ENDPOINT);

        assertThat(loader.load().publicKey().getEncoded())
                .isEqualTo(pair.getPublic().getEncoded());
    }

    @Test
    void anUnreachableStoreIsReportedAsUnreachable() {
        var template = VaultContainerSupport.template(
                VaultContainerSupport.host(), VaultContainerSupport.closedPort(), VaultContainerSupport.ROOT_TOKEN);

        assertThatThrownBy(() -> loader(template).load())
                .satisfies(e -> assertFailure(e, Reason.STORE_UNREACHABLE, "not reachable at " + CONTAINER_ENDPOINT));
    }

    @Test
    void aRejectedCredentialIsReportedAsRejectedAndNotAsUnreachable() {
        seed(RsaKeys.privatePem(pair), RsaKeys.publicPem(pair));
        var template = VaultContainerSupport.template("not-the-token");

        assertThatThrownBy(() -> loader(template).load())
                .satisfies(e -> assertFailure(e, Reason.CREDENTIAL_REJECTED, "rejected the service credential for"));
    }

    @Test
    void aMissingSecretIsReportedWithItsLocation() {
        assertThatThrownBy(() -> loader().load())
                .satisfies(e -> assertFailure(e, Reason.SECRET_NOT_FOUND, "no secret at secret/" + path));
    }

    @Test
    void aMissingPublicKeyNamesTheField() {
        VaultContainerSupport.put(path, Map.of("private-key", RsaKeys.privatePem(pair)));

        assertThatThrownBy(() -> loader().load())
                .satisfies(e -> assertFailure(e, Reason.FIELD_MISSING, "has no public-key"));
    }

    @Test
    void aMissingPrivateKeyNamesTheField() {
        VaultContainerSupport.put(path, Map.of("public-key", RsaKeys.publicPem(pair)));

        assertThatThrownBy(() -> loader().load())
                .satisfies(e -> assertFailure(e, Reason.FIELD_MISSING, "has no private-key"));
    }

    @Test
    void aBlankPublicKeyIsTreatedAsMissing() {
        VaultContainerSupport.put(path, Map.of("private-key", RsaKeys.privatePem(pair), "public-key", "  "));

        assertThatThrownBy(() -> loader().load())
                .satisfies(e -> assertFailure(e, Reason.FIELD_MISSING, "has no public-key"));
    }

    @Test
    void aGarbagePrivateKeyIsInvalidAndLeaksNothing() {
        String garbage = "-----BEGIN PRIVATE KEY-----\nQUJDREVGR0hJSktMTU5PUA==\n-----END PRIVATE KEY-----";
        seed(garbage, RsaKeys.publicPem(pair));

        assertThatThrownBy(() -> loader().load()).satisfies(e -> {
            assertFailure(e, Reason.KEY_INVALID, "private-key is not a valid PKCS#8 RSA private key");
            assertNothingLeaks(e, garbage, RsaKeys.publicPem(pair));
        });
    }

    @Test
    void aGarbagePublicKeyIsInvalidAndLeaksNothing() {
        String garbage = "-----BEGIN PUBLIC KEY-----\nQUJDREVGR0hJSktMTU5PUA==\n-----END PUBLIC KEY-----";
        seed(RsaKeys.privatePem(pair), garbage);

        assertThatThrownBy(() -> loader().load()).satisfies(e -> {
            assertFailure(e, Reason.KEY_INVALID, "public-key is not a valid X.509 RSA public key");
            assertNothingLeaks(e, garbage, RsaKeys.privatePem(pair));
        });
    }

    @Test
    void aMismatchedPairIsReportedAndLeaksNothing() {
        KeyPair other = RsaKeys.newKeyPair();
        seed(RsaKeys.privatePem(pair), RsaKeys.publicPem(other));

        assertThatThrownBy(() -> loader().load()).satisfies(e -> {
            assertFailure(e, Reason.KEY_PAIR_MISMATCH, "do not form a matching key pair");
            assertNothingLeaks(e, RsaKeys.privatePem(pair), RsaKeys.publicPem(other));
        });
    }
}
