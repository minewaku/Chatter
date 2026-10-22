package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Which audience a token names does not decide whether it is accepted; everything else about it still does. */
class AudienceE2eTest extends E2eSupport {

    private static final String PROTECTED = "/sessions";

    private long userId;
    private String sessionId;

    private void signInFreshAccount() {
        String[] tokens = signIn(registerVerifiedAccount());
        userId = Long.parseLong(claim(tokens[0], "sub"));
        sessionId = claim(tokens[0], "sid");
    }

    @Test
    void aTokenNamingAnotherAudienceIsAccepted() {
        signInFreshAccount();

        assertThat(api().get(PROTECTED, TestKeys.tokenWithAudience(userId, sessionId, "another-app"))
                        .status())
                .isEqualTo(200);
    }

    @Test
    void aTokenNamingNoAudienceIsAccepted() {
        signInFreshAccount();

        assertThat(api().get(PROTECTED, TestKeys.tokenWithAudience(userId, sessionId, null))
                        .status())
                .isEqualTo(200);
    }

    @Test
    void theOtherChecksStillRejectTheToken() {
        signInFreshAccount();

        assertThat(api().get(PROTECTED, TestKeys.expiredToken(userId, sessionId))
                        .status())
                .isEqualTo(401);
        assertThat(api().get(PROTECTED, TestKeys.foreignKeyToken(userId, sessionId))
                        .status())
                .isEqualTo(401);
        assertThat(api().get(PROTECTED, TestKeys.wrongIssuerToken(userId, sessionId))
                        .status())
                .isEqualTo(401);
    }

    @Test
    void aFreshlyIssuedTokenStillNamesThePlatformAudience() {
        String[] tokens = signIn(registerVerifiedAccount());

        String payload =
                new String(java.util.Base64.getUrlDecoder().decode(tokens[0].split("\\.")[1]));

        String audience = new tools.jackson.databind.ObjectMapper()
                .readTree(payload)
                .get("aud")
                .toString();
        assertThat(audience).isIn("\"chatter\"", "[\"chatter\"]");
    }
}
