package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionLifecycleE2eTest extends E2eSupport {

    private long storedExpiry(String sessionId) {
        return Long.parseLong((String) redis.opsForHash().get(KEY_PREFIX + "session:" + sessionId, "expiresAt"));
    }

    @Test
    void refreshRotatesTheCredentialAndRenewsTheSession() {
        String email = registerVerifiedAccount();
        String[] first = signIn(email);
        String sessionId = claim(first[0], "sid");
        long expiryBefore = storedExpiry(sessionId);

        ApiResponse renewed = api().post("/auth/refresh", Map.of("refreshToken", first[1]));

        assertThat(renewed.status()).isEqualTo(200);
        assertThat(renewed.json().size()).isEqualTo(2);
        assertThat(renewed.text("refreshToken")).isNotBlank().isNotEqualTo(first[1]);
        assertThat(claim(renewed.text("accessToken"), "sid")).isEqualTo(sessionId);
        assertThat(storedExpiry(sessionId)).isGreaterThan(expiryBefore);
        assertThat(renewed.header("Set-Cookie")).isNull();

        ApiResponse chained = api().post("/auth/refresh", Map.of("refreshToken", renewed.text("refreshToken")));
        assertThat(chained.status()).isEqualTo(200);

        ApiResponse reused = api().post("/auth/refresh", Map.of("refreshToken", first[1]));
        assertThat(reused.status()).isEqualTo(401);
        assertThat(reused.text("code")).isEqualTo("authentication_failed");
    }

    // ---- reuse of a replaced credential (spec 009) ----

    private ApiResponse refresh(String credential) {
        return api().post("/auth/refresh", Map.of("refreshToken", credential));
    }

    private boolean sessionListShowsRevoked(String viewerAccessToken, String sessionId) {
        ApiResponse list = api().get("/sessions", viewerAccessToken);
        for (int i = 0; i < list.json().size(); i++) {
            if (list.json().get(i).get("sessionId").asString().equals(sessionId)) {
                var revokedAt = list.json().get(i).get("revokedAt");
                return revokedAt != null && !revokedAt.isNull();
            }
        }
        throw new IllegalStateException("session not listed: " + sessionId);
    }

    @Test
    void replayingAReplacedCredentialIsRefusedLikeAnUnknownOneAndEndsTheSession() {
        String[] tokens = signIn(registerVerifiedAccount());
        ApiResponse renewed = refresh(tokens[1]);
        String current = renewed.text("refreshToken");

        ApiResponse replayed = refresh(tokens[1]);
        ApiResponse unknown = refresh("never-issued");

        assertThat(replayed.status()).isEqualTo(401);
        assertThat(replayed.status()).isEqualTo(unknown.status());
        assertThat(replayed.text("code")).isEqualTo(unknown.text("code"));
        assertThat(replayed.text("message")).isEqualTo(unknown.text("message"));
        assertThat(refresh(current).status()).isEqualTo(401);
        ApiResponse blocked = api().get("/sessions", renewed.text("accessToken"));
        assertThat(blocked.status()).isEqualTo(401);
        assertThat(blocked.text("code")).isEqualTo("unauthenticated");
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(401);
    }

    @Test
    void reuseEndsOnlyThatSessionAndTheOtherDeviceKeepsWorking() {
        String email = registerVerifiedAccount();
        String[] stolenFrom = signIn(email);
        String[] other = signIn(email);
        refresh(stolenFrom[1]);

        assertThat(refresh(stolenFrom[1]).status()).isEqualTo(401);

        assertThat(api().get("/sessions", other[0]).status()).isEqualTo(200);
        assertThat(refresh(other[1]).status()).isEqualTo(200);
        assertThat(sessionListShowsRevoked(other[0], claim(stolenFrom[0], "sid")))
                .isTrue();
        assertThat(sessionListShowsRevoked(other[0], claim(other[0], "sid"))).isFalse();
    }

    @Test
    void anyEarlierCredentialOfASessionEndsItNotOnlyTheLatestOne() {
        String[] tokens = signIn(registerVerifiedAccount());
        String second = refresh(tokens[1]).text("refreshToken");
        String third = refresh(second).text("refreshToken");
        String fourth = refresh(third).text("refreshToken");

        assertThat(refresh(tokens[1]).status()).isEqualTo(401);

        assertThat(refresh(fourth).status()).isEqualTo(401);
    }

    @Test
    void signingOutWithAReplacedCredentialDoesNotEndTheSession() {
        String[] tokens = signIn(registerVerifiedAccount());
        ApiResponse renewed = refresh(tokens[1]);

        ApiResponse loggedOut = api().post("/auth/logout", Map.of("refreshToken", tokens[1]));

        assertThat(loggedOut.status()).isEqualTo(204);
        assertThat(api().get("/sessions", renewed.text("accessToken")).status()).isEqualTo(200);
        assertThat(refresh(renewed.text("refreshToken")).status()).isEqualTo(200);
    }

    // ---- normal use is unaffected (spec 009) ----

    @Test
    void fiftyRenewalsInOrderAreNeverRefusedAndNeverRevokeTheSession() {
        String[] tokens = signIn(registerVerifiedAccount());
        String sessionId = claim(tokens[0], "sid");
        String credential = tokens[1];
        String access = tokens[0];

        for (int i = 0; i < 50; i++) {
            ApiResponse renewed = refresh(credential);
            assertThat(renewed.status()).as("renewal %s", i + 1).isEqualTo(200);
            assertThat(claim(renewed.text("accessToken"), "sid")).isEqualTo(sessionId);
            credential = renewed.text("refreshToken");
            access = renewed.text("accessToken");
        }

        assertThat(api().get("/sessions", access).status()).isEqualTo(200);
        assertThat(sessionListShowsRevoked(access, sessionId)).isFalse();
    }

    @Test
    void anUnknownOrBlankCredentialIsRefusedWithoutEndingAnySession() {
        String[] tokens = signIn(registerVerifiedAccount());

        assertThat(refresh("never-issued").status()).isEqualTo(401);
        assertThat(refresh("x".repeat(43)).status()).isEqualTo(401);
        assertThat(api().post("/auth/refresh", Map.of()).status()).isNotEqualTo(200);

        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        assertThat(refresh(tokens[1]).status()).isEqualTo(200);
    }

    @Test
    void anExpiredOrInvalidAuthorizationHeaderOnAPublicEndpointIsIgnored() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse renewed = api().post("/auth/refresh", Map.of("refreshToken", tokens[1]), "not.a.valid-token");

        assertThat(renewed.status()).isEqualTo(200);
    }

    @Test
    void signingOutOneDeviceBlocksItsAccessTokenImmediatelyAndLeavesTheOtherDeviceAlone() {
        String email = registerVerifiedAccount();
        String[] device1 = signIn(email);
        String[] device2 = signIn(email);
        assertThat(api().get("/sessions", device1[0]).status()).isNotEqualTo(401);

        ApiResponse loggedOut = api().post("/auth/logout", Map.of("refreshToken", device1[1]));

        assertThat(loggedOut.status()).isEqualTo(204);
        ApiResponse blocked = api().get("/sessions", device1[0]);
        assertThat(blocked.status()).isEqualTo(401);
        assertThat(blocked.text("code")).isEqualTo("unauthenticated");
        assertThat(api().get("/sessions", device2[0]).status()).isNotEqualTo(401);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", device2[1]))
                        .status())
                .isEqualTo(200);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", device1[1]))
                        .status())
                .isEqualTo(401);
    }

    @Test
    void signingOutTwiceOrWithAnUnknownCredentialIsTheSameSuccess() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse first = api().post("/auth/logout", Map.of("refreshToken", tokens[1]));
        ApiResponse second = api().post("/auth/logout", Map.of("refreshToken", tokens[1]));
        ApiResponse unknown = api().post("/auth/logout", Map.of("refreshToken", "never-issued"));
        ApiResponse missing = api().post("/auth/logout", Map.of());

        for (ApiResponse response : new ApiResponse[] {first, second, unknown, missing}) {
            assertThat(response.status()).isEqualTo(204);
            assertThat(response.body()).isEmpty();
        }
    }

    @Test
    void losingTheRevocationEntriesNeverLetsARevokedSessionRenew() {
        String[] tokens = signIn(registerVerifiedAccount());
        api().post("/auth/logout", Map.of("refreshToken", tokens[1]));
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(401);

        redis.delete(redis.keys(KEY_PREFIX + "access-revoked:*"));

        // the access token works again only until its own expiry; the session record stays revoked
        assertThat(api().get("/sessions", tokens[0]).status()).isNotEqualTo(401);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", tokens[1]))
                        .status())
                .isEqualTo(401);
    }

    @Test
    void anAccessTokenWithoutASessionClaimIsRefused() {
        // a token signed with the right key but issued before this feature has no session claim
        String legacy = TestKeys.legacyAccessToken(987654L, "legacy@example.com");

        ApiResponse response = api().get("/sessions", legacy);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.text("code")).isEqualTo("unauthenticated");
    }
}
