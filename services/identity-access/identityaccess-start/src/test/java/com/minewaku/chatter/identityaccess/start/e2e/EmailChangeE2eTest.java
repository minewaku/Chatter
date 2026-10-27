package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The mail never leaves memory here: {@link RecordingMailSender} captures it and the test reads the token from it. */
class EmailChangeE2eTest extends E2eSupport {

    private ApiResponse requestChange(String accessToken, String newEmail, String password) {
        return api().post(
                        "/users/@me/email-change",
                        Map.of("newEmail", newEmail, "currentPassword", password),
                        accessToken);
    }

    private ApiResponse confirm(String token) {
        return api().get("/users/email-change/confirm?token=" + token, null);
    }

    private int signInStatus(String email) {
        return api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD))
                .status();
    }

    @Test
    void theOldAddressStaysInEffectUntilTheNewOneIsConfirmed() {
        String oldEmail = registerVerifiedAccount();
        String newEmail = uniqueEmail();
        String[] tokens = signIn(oldEmail);

        ApiResponse accepted = requestChange(tokens[0], newEmail, PASSWORD);

        assertThat(accepted.status()).isEqualTo(202);
        assertThat(accepted.body()).isEmpty();
        assertThat(mail.countFor(newEmail)).isEqualTo(1);
        assertThat(signInStatus(oldEmail)).isEqualTo(200);
        assertThat(signInStatus(newEmail)).isEqualTo(401);

        String token = mail.latestTokenFor(newEmail).orElseThrow();
        ApiResponse confirmed = confirm(token);

        assertThat(confirmed.status()).isEqualTo(200);
        assertThat(confirmed.text("status")).isEqualTo("confirmed");
        assertThat(confirmed.header("Cache-Control")).contains("no-store");
        assertThat(signInStatus(newEmail)).isEqualTo(200);
        assertThat(signInStatus(oldEmail)).isEqualTo(401);

        ApiResponse again = confirm(token);
        assertThat(again.status()).isEqualTo(200);
        assertThat(again.text("status")).isEqualTo("already_confirmed");
    }

    @Test
    void theRequestAndTheConfirmationDoNotEndTheCallersSessions() {
        String oldEmail = registerVerifiedAccount();
        String newEmail = uniqueEmail();
        String[] tokens = signIn(oldEmail);

        requestChange(tokens[0], newEmail, PASSWORD);
        confirm(mail.latestTokenFor(newEmail).orElseThrow());

        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        assertThat(api().refresh(tokens[1])
                        .status())
                .isEqualTo(200);
    }

    @Test
    void aSecondRequestReplacesTheFirstAndTheFirstTokenStopsWorking() {
        String oldEmail = registerVerifiedAccount();
        String first = uniqueEmail();
        String second = uniqueEmail();
        String[] tokens = signIn(oldEmail);

        requestChange(tokens[0], first, PASSWORD);
        String firstToken = mail.latestTokenFor(first).orElseThrow();
        requestChange(tokens[0], second, PASSWORD);
        String secondToken = mail.latestTokenFor(second).orElseThrow();

        ApiResponse stale = confirm(firstToken);
        assertThat(stale.status()).isEqualTo(400);
        assertThat(stale.text("code")).isEqualTo("invalid_or_expired_token");
        assertThat(confirm(secondToken).status()).isEqualTo(200);
        assertThat(signInStatus(second)).isEqualTo(200);
        assertThat(signInStatus(first)).isEqualTo(401);
    }

    @Test
    void aWrongPasswordIsForbiddenSendsNothingAndTheCallerStaysSignedIn() {
        String oldEmail = registerVerifiedAccount();
        String newEmail = uniqueEmail();
        String[] tokens = signIn(oldEmail);

        for (String wrong : new String[] {"Wrong-pass1!", "wrong"}) {
            ApiResponse refused = requestChange(tokens[0], newEmail, wrong);

            assertThat(refused.status()).isEqualTo(403);
            assertThat(refused.text("code")).isEqualTo("reauthentication_failed");
            assertThat(refused.body()).doesNotContain(wrong).doesNotContain(newEmail);
        }

        assertThat(mail.countFor(newEmail)).isZero();
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
        assertThat(api().refresh(tokens[1])
                        .status())
                .isEqualTo(200);
    }

    @Test
    void theSameEmailIsAConflictAndSendsNothing() {
        String email = registerVerifiedAccount();
        String[] tokens = signIn(email);
        int before = mail.countFor(email);

        ApiResponse same = requestChange(tokens[0], email.toUpperCase(), PASSWORD);

        assertThat(same.status()).isEqualTo(409);
        assertThat(same.text("code")).isEqualTo("email_unchanged");
        assertThat(mail.countFor(email)).isEqualTo(before);
    }

    @Test
    void anAddressHeldByAnotherAccountIsAConflictThatNamesNobodyAndSendsNothing() {
        String taken = registerVerifiedAccount();
        String[] mine = signIn(registerVerifiedAccount());
        int before = mail.countFor(taken);

        ApiResponse clash = requestChange(mine[0], taken, PASSWORD);

        assertThat(clash.status()).isEqualTo(409);
        assertThat(clash.text("code")).isEqualTo("account_already_exists");
        assertThat(clash.body()).doesNotContain(taken);
        assertThat(mail.countFor(taken)).isEqualTo(before);
    }

    @Test
    void anInvalidEmailIsReportedAgainstItsFieldWithoutEchoingIt() {
        String[] tokens = signIn(registerVerifiedAccount());

        ApiResponse rejected = requestChange(tokens[0], "not-an-email", PASSWORD);

        assertThat(rejected.status()).isEqualTo(400);
        assertThat(rejected.text("code")).isEqualTo("validation_failed");
        assertThat(rejected.body()).doesNotContain("not-an-email");
    }

    @Test
    void anAddressTakenBeforeConfirmationIsAConflictAndTheOldAddressKeepsWorking() {
        String oldEmail = registerVerifiedAccount();
        String wanted = uniqueEmail();
        String[] tokens = signIn(oldEmail);
        requestChange(tokens[0], wanted, PASSWORD);
        String token = mail.latestTokenFor(wanted).orElseThrow();
        // someone else registers the address in the meantime
        ApiResponse registered = api().post(
                        "/auth/register",
                        Map.of(
                                "email",
                                wanted,
                                "username",
                                uniqueUsername(),
                                "birthday",
                                "1999-01-01",
                                "password",
                                PASSWORD));
        assertThat(registered.status()).isEqualTo(201);

        ApiResponse clash = confirm(token);

        assertThat(clash.status()).isEqualTo(409);
        assertThat(clash.text("code")).isEqualTo("account_already_exists");
        assertThat(signInStatus(oldEmail)).isEqualTo(200);
        assertThat(confirm(token).status()).isEqualTo(400);
    }

    @Test
    void aRegistrationTokenAndAnEmailChangeTokenAreNotInterchangeable() {
        String pendingEmail = uniqueEmail();
        api().post(
                        "/auth/register",
                        Map.of(
                                "email",
                                pendingEmail,
                                "username",
                                uniqueUsername(),
                                "birthday",
                                "1999-01-01",
                                "password",
                                PASSWORD));
        String registrationToken = mail.latestTokenFor(pendingEmail).orElseThrow();
        String oldEmail = registerVerifiedAccount();
        String newEmail = uniqueEmail();
        String[] tokens = signIn(oldEmail);
        requestChange(tokens[0], newEmail, PASSWORD);
        String changeToken = mail.latestTokenFor(newEmail).orElseThrow();

        assertThat(confirm(registrationToken).status()).isEqualTo(400);
        assertThat(api().get("/auth/verification/confirm?token=" + changeToken, null)
                        .status())
                .isEqualTo(400);
        // neither mistaken use consumed the real token
        assertThat(signInStatus(pendingEmail)).isEqualTo(401);
        assertThat(confirm(changeToken).status()).isEqualTo(200);
    }

    @Test
    void closingTheAccountDiscardsAPendingChange() {
        String oldEmail = registerVerifiedAccount();
        String newEmail = uniqueEmail();
        String[] tokens = signIn(oldEmail);
        requestChange(tokens[0], newEmail, PASSWORD);
        String token = mail.latestTokenFor(newEmail).orElseThrow();

        assertThat(api().post("/users/@me/close", Map.of("currentPassword", PASSWORD), tokens[0])
                        .status())
                .isEqualTo(204);

        ApiResponse late = confirm(token);
        assertThat(late.status()).isEqualTo(400);
        assertThat(late.text("code")).isEqualTo("invalid_or_expired_token");
        assertThat(signInStatus(newEmail)).isEqualTo(401);
    }

    @Test
    void theConfirmationIsPublicAndIgnoresABadAuthorizationHeader() {
        String oldEmail = registerVerifiedAccount();
        String newEmail = uniqueEmail();
        String[] tokens = signIn(oldEmail);
        requestChange(tokens[0], newEmail, PASSWORD);
        String token = mail.latestTokenFor(newEmail).orElseThrow();

        ApiResponse confirmed = api().get("/users/email-change/confirm?token=" + token, "not-a-real-token");

        assertThat(confirmed.status()).isEqualTo(200);
        assertThat(confirmed.text("status")).isEqualTo("confirmed");
    }

    @Test
    void blankAndUnknownTokensAreInvalid() {
        assertThat(api().get("/users/email-change/confirm", null).status()).isEqualTo(400);
        assertThat(confirm("does-not-exist").status()).isEqualTo(400);
        assertThat(confirm("%20").status()).isEqualTo(400);
    }

    @Test
    void requestingAnEmailChangeWithoutACredentialIsUnauthenticated() {
        ApiResponse response = requestChange(null, uniqueEmail(), PASSWORD);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.text("code")).isEqualTo("unauthenticated");
    }
}
