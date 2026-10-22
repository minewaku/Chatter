package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The properties the service used to read its keys from are still set, with values that would not verify anything.
 * They have no effect: the Vault pair is the one in use.
 */
class LeftoverKeysE2eTest extends E2eSupport {

    @DynamicPropertySource
    static void leftoverKeyProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "identity-access.security.jwt.private-key",
                () -> "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----");
        registry.add(
                "identity-access.security.jwt.public-key",
                () -> "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----");
    }

    @Test
    void theServiceStartsAndUsesTheVaultPair() {
        String[] tokens = signIn(registerVerifiedAccount());
        long userId = Long.parseLong(claim(tokens[0], "sub"));
        String sessionId = claim(tokens[0], "sid");

        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        // a token signed with the Vault pair (the pair the tests seeded) is accepted; one signed elsewhere is not
        assertThat(api().get("/sessions", TestKeys.tokenWithAudience(userId, sessionId, "chatter"))
                        .status())
                .isEqualTo(200);
        assertThat(api().get("/sessions", TestKeys.foreignKeyToken(userId, sessionId))
                        .status())
                .isEqualTo(401);
    }
}
