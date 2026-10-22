package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionManagementE2eTest extends E2eSupport {

    private static final String FIREFOX =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0";

    private String[] signInAs(String email, String userAgent) {
        ApiResponse response =
                api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD), null, userAgent);
        assertThat(response.status()).isEqualTo(200);
        return new String[] {response.text("accessToken"), response.text("refreshToken")};
    }

    private ApiResponse revoke(String accessToken, String sessionId, String password) {
        return api().post("/sessions/" + sessionId + "/revoke", Map.of("currentPassword", password), accessToken);
    }

    @Test
    void listShowsOnlyTheCallersSessionsNewestFirstWithoutAnyCredential() {
        String email = registerVerifiedAccount();
        String[] first = signInAs(email, FIREFOX);
        String[] second = signIn(email);
        signIn(registerVerifiedAccount()); // another account's session must never appear

        ApiResponse list = api().get("/sessions", first[0]);

        assertThat(list.status()).isEqualTo(200);
        assertThat(list.json().size()).isEqualTo(2);
        assertThat(list.json().get(0).get("sessionId").asString()).isEqualTo(claim(second[0], "sid"));
        assertThat(list.json().get(1).get("sessionId").asString()).isEqualTo(claim(first[0], "sid"));
        assertThat(list.json().get(1).get("deviceInfo").asString()).contains("Firefox");
        assertThat(list.body())
                .doesNotContain(first[1])
                .doesNotContain(second[1])
                .doesNotContain("tokenDigest")
                .doesNotContain("refreshToken")
                .doesNotContain("userId");
    }

    @Test
    void aWrongPasswordIsForbiddenNotUnauthenticatedAndChangesNothing() {
        String email = registerVerifiedAccount();
        String[] current = signIn(email);
        String[] other = signIn(email);

        ApiResponse wrong = revoke(current[0], claim(other[0], "sid"), "Wrong-pass1!");
        ApiResponse wrongOthers =
                api().post("/sessions/revoke-others", Map.of("currentPassword", "Wrong-pass1!"), current[0]);

        for (ApiResponse response : new ApiResponse[] {wrong, wrongOthers}) {
            assertThat(response.status()).isEqualTo(403);
            assertThat(response.text("code")).isEqualTo("reauthentication_failed");
        }
        assertThat(api().get("/sessions", other[0]).status()).isEqualTo(200);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", other[1])).status())
                .isEqualTo(200);
    }

    @Test
    void endingASessionBlocksItsCredentialsImmediately() {
        String email = registerVerifiedAccount();
        String[] current = signIn(email);
        String[] other = signIn(email);

        ApiResponse ended = revoke(current[0], claim(other[0], "sid"), PASSWORD);

        assertThat(ended.status()).isEqualTo(204);
        assertThat(api().get("/sessions", other[0]).status()).isEqualTo(401);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", other[1])).status())
                .isEqualTo(401);
        assertThat(api().get("/sessions", current[0]).status()).isEqualTo(200);
        ApiResponse again = revoke(current[0], claim(other[0], "sid"), PASSWORD);
        assertThat(again.status()).isEqualTo(404);
    }

    @Test
    void anUnknownOrForeignSessionIsNotFoundWithoutAnOwnershipHint() {
        String[] mine = signIn(registerVerifiedAccount());
        String[] theirs = signIn(registerVerifiedAccount());

        ApiResponse foreign = revoke(mine[0], claim(theirs[0], "sid"), PASSWORD);
        ApiResponse unknown = revoke(mine[0], "does-not-exist", PASSWORD);

        for (ApiResponse response : new ApiResponse[] {foreign, unknown}) {
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.text("code")).isEqualTo("session_not_found");
        }
        assertThat(api().get("/sessions", theirs[0]).status()).isEqualTo(200);
    }

    @Test
    void theCurrentSessionCanBeEndedToo() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse ended = revoke(tokens[0], claim(tokens[0], "sid"), PASSWORD);

        assertThat(ended.status()).isEqualTo(204);
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(401);
    }

    @Test
    void endingOtherSessionsSparesTheCurrentOneAndIsRepeatable() {
        String email = registerVerifiedAccount();
        String[] current = signIn(email);
        String[] second = signIn(email);
        String[] third = signIn(email);

        ApiResponse first = api().post("/sessions/revoke-others", Map.of("currentPassword", PASSWORD), current[0]);
        ApiResponse repeat = api().post("/sessions/revoke-others", Map.of("currentPassword", PASSWORD), current[0]);

        assertThat(first.status()).isEqualTo(200);
        assertThat(first.json().get("revokedCount").asInt()).isEqualTo(2);
        assertThat(repeat.json().get("revokedCount").asInt()).isZero();
        assertThat(api().get("/sessions", current[0]).status()).isEqualTo(200);
        for (String[] ended : new String[][] {second, third}) {
            assertThat(api().get("/sessions", ended[0]).status()).isEqualTo(401);
            assertThat(api().post("/auth/refresh", Map.of("refreshToken", ended[1]))
                            .status())
                    .isEqualTo(401);
        }
    }

    @Test
    void aMissingPasswordIsAValidationFailureAndNoCredentialIsUnauthenticated() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse missing = api().post("/sessions/revoke-others", Map.of(), tokens[0]);
        ApiResponse anonymous = api().post("/sessions/revoke-others", Map.of("currentPassword", PASSWORD));

        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.text("code")).isEqualTo("validation_failed");
        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.text("code")).isEqualTo("unauthenticated");
    }
}
