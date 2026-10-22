package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AccountManagementE2eTest extends E2eSupport {

    private static final String NEW_PASSWORD = "New-pass-2@";

    private ApiResponse changePassword(String accessToken, String current, String next) {
        return api().put("/users/@me/password", Map.of("currentPassword", current, "newPassword", next), accessToken);
    }

    @Test
    void changingThePasswordEndsEverySessionAndOnlyTheNewPasswordSignsIn() {
        String email = registerVerifiedAccount();
        String[] device1 = signIn(email);
        String[] device2 = signIn(email);

        ApiResponse changed = changePassword(device1[0], PASSWORD, NEW_PASSWORD);

        assertThat(changed.status()).isEqualTo(204);
        assertThat(changed.body()).isEmpty();
        assertThat(api().get("/sessions", device1[0]).status()).isEqualTo(401);
        assertThat(api().get("/sessions", device2[0]).status()).isEqualTo(401);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", device2[1]))
                        .status())
                .isEqualTo(401);
        assertThat(api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD))
                        .status())
                .isEqualTo(401);
        ApiResponse signedIn = api().post("/auth/authenticate", ApiClient.credentials(email, NEW_PASSWORD));
        assertThat(signedIn.status()).isEqualTo(200);
    }

    @Test
    void aWrongCurrentPasswordIsForbiddenAndChangesNothing() {
        String email = registerVerifiedAccount();
        String[] tokens = signIn(email);

        ApiResponse refused = changePassword(tokens[0], "Wrong-pass1!", NEW_PASSWORD);

        assertThat(refused.status()).isEqualTo(403);
        assertThat(refused.text("code")).isEqualTo("reauthentication_failed");
        assertThat(refused.body()).doesNotContain("Wrong-pass1!").doesNotContain(NEW_PASSWORD);
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        assertThat(api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD))
                        .status())
                .isEqualTo(200);
    }

    @Test
    void aWeakNewPasswordIsReportedAgainstItsFieldWithoutEchoingIt() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse weak = changePassword(tokens[0], PASSWORD, "weakpw1");

        assertThat(weak.status()).isEqualTo(400);
        assertThat(weak.text("code")).isEqualTo("validation_failed");
        assertThat(weak.json().get("errors").get(0).get("field").asString()).isEqualTo("newPassword");
        assertThat(weak.body()).doesNotContain("weakpw1");
    }

    @Test
    void theSamePasswordIsAConflict() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse same = changePassword(tokens[0], PASSWORD, PASSWORD);

        assertThat(same.status()).isEqualTo(409);
        assertThat(same.text("code")).isEqualTo("password_unchanged");
    }

    @Test
    void closingTheAccountEndsItsSessionsAndBlocksSignIn() {
        String email = registerVerifiedAccount();
        String[] tokens = signIn(email);

        ApiResponse closed = api().post("/users/@me/close", Map.of("currentPassword", PASSWORD), tokens[0]);

        assertThat(closed.status()).isEqualTo(204);
        assertThat(closed.body()).isEmpty();
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(401);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", tokens[1]))
                        .status())
                .isEqualTo(401);
        assertThat(api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD))
                        .status())
                .isEqualTo(401);
        // the credential is gone, so closing again is not possible
        assertThat(api().post("/users/@me/close", Map.of("currentPassword", PASSWORD), tokens[0])
                        .status())
                .isEqualTo(401);
    }

    @Test
    void aWrongPasswordWhenClosingIsForbiddenAndTheCallerStaysSignedIn() {
        String email = registerVerifiedAccount();
        String[] tokens = signIn(email);

        for (String wrong : new String[] {"Wrong-pass1!", "wrong"}) {
            ApiResponse refused = api().post("/users/@me/close", Map.of("currentPassword", wrong), tokens[0]);

            assertThat(refused.status()).isEqualTo(403);
            assertThat(refused.text("code")).isEqualTo("reauthentication_failed");
            assertThat(refused.body()).doesNotContain(wrong);
        }
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", tokens[1]))
                        .status())
                .isEqualTo(200);
        assertThat(api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD))
                        .status())
                .isEqualTo(200);
    }

    @Test
    void closingWithoutAPasswordIsRejectedAndTheBodylessFormIsGone() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse missing = api().post("/users/@me/close", Map.of(), tokens[0]);
        ApiResponse blank = api().post("/users/@me/close", Map.of("currentPassword", " "), tokens[0]);

        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.text("code")).isEqualTo("validation_failed");
        assertThat(blank.status()).isEqualTo(400);
        assertThat(api().delete("/users/@me", tokens[0]).status()).isEqualTo(404);
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
    }

    private ApiResponse changeUsername(String accessToken, String username) {
        return api().put("/users/@me/username", Map.of("username", username), accessToken);
    }

    @Test
    void changingTheUsernameNeedsNoPasswordAndFreesTheOldName() {
        String oldName = uniqueUsername();
        String newName = uniqueUsername();
        String[] tokens = signIn(registerVerifiedAccount(oldName));

        ApiResponse changed = changeUsername(tokens[0], newName);

        assertThat(changed.status()).isEqualTo(204);
        assertThat(changed.body()).isEmpty();
        // the old name is free again, the new one is taken
        assertThat(registerWithUsername(oldName)).isEqualTo(201);
        assertThat(registerWithUsername(newName)).isEqualTo(409);
        // sessions are not ended by a username change
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        assertThat(api().post("/auth/refresh", Map.of("refreshToken", tokens[1]))
                        .status())
                .isEqualTo(200);
    }

    @Test
    void theSameUsernameAgainIsAConflict() {
        String name = uniqueUsername();
        String[] tokens = signIn(registerVerifiedAccount(name));

        ApiResponse same = changeUsername(tokens[0], name);

        assertThat(same.status()).isEqualTo(409);
        assertThat(same.text("code")).isEqualTo("username_unchanged");
    }

    @Test
    void aUsernameHeldByAnotherAccountIsAConflictThatNamesNobody() {
        String taken = uniqueUsername();
        registerVerifiedAccount(taken);
        String[] mine = signIn(registerVerifiedAccount());

        ApiResponse clash = changeUsername(mine[0], taken);

        assertThat(clash.status()).isEqualTo(409);
        assertThat(clash.text("code")).isEqualTo("account_already_exists");
        assertThat(clash.body()).doesNotContain(taken);
        assertThat(api().get("/sessions", mine[0]).status()).isEqualTo(200);
    }

    @Test
    void anInvalidUsernameIsReportedAgainstItsFieldWithoutEchoingIt() {
        String[] tokens = signIn(registerVerifiedAccount());
        String tooLong = "n".repeat(66);

        ApiResponse rejected = changeUsername(tokens[0], tooLong);

        assertThat(rejected.status()).isEqualTo(400);
        assertThat(rejected.text("code")).isEqualTo("validation_failed");
        assertThat(rejected.body()).doesNotContain(tooLong);
        assertThat(changeUsername(tokens[0], ".bad").status()).isEqualTo(400);
    }

    @Test
    void changingTheUsernameWithoutACredentialIsUnauthenticated() {
        assertThat(changeUsername(null, uniqueUsername()).status()).isEqualTo(401);
    }

    private int registerWithUsername(String username) {
        return api().post(
                        "/auth/register",
                        Map.of(
                                "email",
                                uniqueEmail(),
                                "username",
                                username,
                                "birthday",
                                "1999-01-01",
                                "password",
                                PASSWORD))
                .status();
    }

    @Test
    void anotherAccountIsNeverTouchedByTheCallersRequest() {
        String[] mine = signIn(registerVerifiedAccount());
        String[] theirs = signIn(registerVerifiedAccount());

        api().post("/users/@me/close?userId=1", Map.of("currentPassword", PASSWORD), mine[0]);

        assertThat(api().get("/sessions", theirs[0]).status()).isEqualTo(200);
    }
}
