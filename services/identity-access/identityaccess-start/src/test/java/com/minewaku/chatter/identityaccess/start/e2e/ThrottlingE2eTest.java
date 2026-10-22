package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs on its own context with small limits and short windows, so a limit can be reached and then waited out. The
 * other end-to-end tests keep generous limits and never see a {@code 429}.
 */
class ThrottlingE2eTest extends E2eSupport {

    @DynamicPropertySource
    static void limits(DynamicPropertyRegistry registry) {
        registry.add("identity-access.throttle.sign-in.max-attempts", () -> "3");
        registry.add("identity-access.throttle.sign-in.window", () -> "PT3S");
        registry.add("identity-access.throttle.reconfirmation.max-attempts", () -> "3");
        registry.add("identity-access.throttle.reconfirmation.window", () -> "PT3S");
        registry.add("identity-access.throttle.resend-gap.max-attempts", () -> "1");
        registry.add("identity-access.throttle.resend-gap.window", () -> "PT2S");
        registry.add("identity-access.throttle.resend-hourly.max-attempts", () -> "3");
        registry.add("identity-access.throttle.email-change-request.max-attempts", () -> "2");
    }

    private ApiResponse signInAttempt(String email, String password) {
        return api().post("/auth/authenticate", ApiClient.credentials(email, password));
    }

    @Test
    void theFourthSignInForOneAccountIsRefusedEvenWithTheRightPassword() {
        String email = registerVerifiedAccount();

        for (int i = 0; i < 3; i++) {
            assertThat(signInAttempt(email, "Wrong-password1").status()).isEqualTo(401);
        }
        ApiResponse refused = signInAttempt(email, PASSWORD);

        assertThat(refused.status()).isEqualTo(429);
        assertThat(refused.text("code")).isEqualTo("too_many_attempts");
        assertThat(Integer.parseInt(refused.header("Retry-After"))).isBetween(1, 3);
        assertThat(refused.body()).doesNotContain(email);
    }

    @Test
    void anEmailWithNoAccountIsRefusedWithTheSameShape() {
        String email = uniqueEmail();

        for (int i = 0; i < 3; i++) {
            assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(401);
        }
        ApiResponse refused = signInAttempt(email, PASSWORD);

        assertThat(refused.status()).isEqualTo(429);
        assertThat(refused.text("code")).isEqualTo("too_many_attempts");
        assertThat(refused.json().propertyNames()).containsExactlyInAnyOrder("status", "code", "message", "timestamp");
    }

    @Test
    void theOwnerCanSignInAgainOnceTheWindowHasPassed() throws InterruptedException {
        String email = registerVerifiedAccount();
        for (int i = 0; i < 3; i++) {
            signInAttempt(email, "Wrong-password1");
        }
        assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(429);

        Thread.sleep(3500);

        assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(200);
    }

    @Test
    void aSuccessfulSignInClearsTheCount() {
        String email = registerVerifiedAccount();
        signInAttempt(email, "Wrong-password1");
        signInAttempt(email, "Wrong-password1");

        assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(200);

        for (int i = 0; i < 3; i++) {
            assertThat(signInAttempt(email, "Wrong-password1").status()).isEqualTo(401);
        }
        assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(429);
    }

    @Test
    void requestsWithoutAUsableEmailAreNotCounted() {
        String email = registerVerifiedAccount();

        for (int i = 0; i < 5; i++) {
            // not an address: denied as before, but there is no subject to count
            assertThat(api().post("/auth/authenticate", Map.of("email", "not-an-email", "password", PASSWORD))
                            .status())
                    .isEqualTo(401);
            // blank: rejected by request validation before the service is reached
            assertThat(api().post("/auth/authenticate", Map.of("email", "", "password", PASSWORD))
                            .status())
                    .isEqualTo(400);
        }

        assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(200);
    }

    @Test
    void wrongPasswordsAcrossDifferentActionsShareOneCountAndNeverEndTheSession() throws InterruptedException {
        String[] tokens = signIn(registerVerifiedAccount());
        String access = tokens[0];

        assertThat(api().put(
                                "/users/@me/password",
                                Map.of("currentPassword", "Wrong-password1", "newPassword", "New-pass-2@"),
                                access)
                        .status())
                .isEqualTo(403);
        assertThat(api().put(
                                "/users/@me/password",
                                Map.of("currentPassword", "Wrong-password1", "newPassword", "New-pass-2@"),
                                access)
                        .status())
                .isEqualTo(403);
        ApiResponse wrongClose = api().post("/users/@me/close", Map.of("currentPassword", "Wrong-password1"), access);
        assertThat(wrongClose.status()).isEqualTo(403);
        assertThat(wrongClose.text("code")).isEqualTo("reauthentication_failed");

        ApiResponse refused = api().post("/sessions/revoke-others", Map.of("currentPassword", PASSWORD), access);

        assertThat(refused.status()).isEqualTo(429);
        assertThat(refused.text("code")).isEqualTo("too_many_attempts");
        assertThat(Integer.parseInt(refused.header("Retry-After"))).isBetween(1, 3);
        assertThat(refused.body()).doesNotContain(PASSWORD);
        // a throttled confirmation never ends the session
        assertThat(api().get("/sessions", access).status()).isEqualTo(200);

        Thread.sleep(3500);

        assertThat(api().post("/sessions/revoke-others", Map.of("currentPassword", PASSWORD), access)
                        .status())
                .isEqualTo(200);
    }

    private String registerPending() {
        String email = uniqueEmail();
        ApiResponse registered = api().post(
                        "/auth/register",
                        Map.of(
                                "email",
                                email,
                                "username",
                                uniqueUsername(),
                                "birthday",
                                "1999-01-01",
                                "password",
                                PASSWORD));
        assertThat(registered.status()).isEqualTo(201);
        return email;
    }

    private ApiResponse resend(String email) {
        return api().post("/auth/verification/resend", Map.of("email", email));
    }

    @Test
    void aSecondResendInsideTheGapIsRefusedAndSendsNoMail() {
        String email = registerPending();
        int sentAtRegistration = mail.countFor(email);

        ApiResponse first = resend(email);
        ApiResponse second = resend(email);

        assertThat(first.status()).isEqualTo(202);
        assertThat(second.status()).isEqualTo(429);
        assertThat(second.text("code")).isEqualTo("too_many_attempts");
        assertThat(second.header("Retry-After")).isNotNull();
        assertThat(mail.countFor(email)).isEqualTo(sentAtRegistration + 1);
    }

    @Test
    void anAddressWithNoAccountGetsTheSameAnswersAsAPendingOne() {
        String pending = registerPending();
        String unknown = uniqueEmail();

        ApiResponse knownFirst = resend(pending);
        ApiResponse unknownFirst = resend(unknown);
        ApiResponse knownSecond = resend(pending);
        ApiResponse unknownSecond = resend(unknown);

        assertThat(unknownFirst.status()).isEqualTo(knownFirst.status()).isEqualTo(202);
        assertThat(unknownSecond.status()).isEqualTo(knownSecond.status()).isEqualTo(429);
        assertThat(unknownSecond.text("code")).isEqualTo(knownSecond.text("code"));
        assertThat(mail.countFor(unknown)).isZero();
    }

    @Test
    void theHourlyCapRefusesTheFourthResendOnceTheGapHasPassed() throws InterruptedException {
        String email = registerPending();

        for (int i = 0; i < 3; i++) {
            assertThat(resend(email).status()).isEqualTo(202);
            Thread.sleep(2200);
        }
        ApiResponse capped = resend(email);

        assertThat(capped.status()).isEqualTo(429);
        assertThat(capped.text("code")).isEqualTo("too_many_attempts");
    }

    @Test
    void aThirdEmailChangeRequestInTheHourIsRefusedAndMailsNothing() {
        String[] tokens = signIn(registerVerifiedAccount());
        String first = uniqueEmail();
        String second = uniqueEmail();
        String third = uniqueEmail();

        assertThat(api().post(
                                "/users/@me/email-change",
                                Map.of("newEmail", first, "currentPassword", PASSWORD),
                                tokens[0])
                        .status())
                .isEqualTo(202);
        assertThat(api().post(
                                "/users/@me/email-change",
                                Map.of("newEmail", second, "currentPassword", PASSWORD),
                                tokens[0])
                        .status())
                .isEqualTo(202);
        ApiResponse refused = api().post(
                        "/users/@me/email-change", Map.of("newEmail", third, "currentPassword", PASSWORD), tokens[0]);

        assertThat(refused.status()).isEqualTo(429);
        assertThat(refused.text("code")).isEqualTo("too_many_attempts");
        assertThat(mail.countFor(third)).isZero();
    }

    @Test
    void anUnreachableStoreFailsSignInClosedInsteadOfLeavingItUnlimited() {
        String email = registerVerifiedAccount();

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        ApiResponse unavailable;
        try {
            unavailable = signInAttempt(email, PASSWORD);
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
            resetBreakers();
        }

        assertThat(unavailable.status()).isEqualTo(503);
        assertThat(unavailable.text("code")).isEqualTo("service_unavailable");
        assertThat(unavailable.header("Retry-After")).isNotNull();
        assertThat(signInAttempt(email, PASSWORD).status()).isEqualTo(200);
    }
}
