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

        ApiResponse reused = api().post("/auth/refresh", Map.of("refreshToken", first[1]));
        assertThat(reused.status()).isEqualTo(401);
        assertThat(reused.text("code")).isEqualTo("authentication_failed");

        ApiResponse chained = api().post("/auth/refresh", Map.of("refreshToken", renewed.text("refreshToken")));
        assertThat(chained.status()).isEqualTo(200);
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
