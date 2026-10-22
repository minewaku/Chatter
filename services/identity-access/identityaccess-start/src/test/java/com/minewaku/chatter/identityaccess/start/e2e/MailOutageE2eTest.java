package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * A failing mail provider must not break registration, must not leave requests waiting once sending is held off, and
 * must recover without a restart. The breaker opens after three recorded sends and stays open for three seconds so the
 * whole cycle fits in a test. Runs in its own context.
 */
@TestPropertySource(
        properties = {
            "identity-access.resilience.mail.window-size=3",
            "identity-access.resilience.mail.minimum-calls=3",
            "identity-access.resilience.mail.open-duration=PT3S"
        })
class MailOutageE2eTest extends E2eSupport {

    @AfterEach
    void restoreMail() {
        mail.failSending(false);
        resetBreakers();
    }

    private ApiResponse register(String email) {
        return api().post(
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
    }

    private void openTheBreaker() {
        for (int i = 0; i < 3; i++) {
            register(uniqueEmail());
        }
    }

    @Test
    void registrationStillCreatesThePendingAccountWhileMailFailsAndAnswersQuicklyOnceSendingIsHeldOff() {
        mail.failSending(true);
        ApiResponse whileRetrying = register(uniqueEmail());
        assertThat(whileRetrying.status()).isEqualTo(201);
        assertThat(whileRetrying.json().get("verificationMailSent").asBoolean()).isFalse();
        openTheBreaker();

        String email = uniqueEmail();
        long started = System.nanoTime();
        ApiResponse heldOff = register(email);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertThat(heldOff.status()).isEqualTo(201);
        assertThat(heldOff.json().get("verificationMailSent").asBoolean()).isFalse();
        assertThat(elapsedMillis).isLessThan(2000);
        assertThat(mail.countFor(email)).isZero();
        // the account exists and is waiting for verification
        ApiResponse again = register(email);
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.text("code")).isEqualTo("verification_pending");
    }

    @Test
    void aResendIsStillAcceptedWhileMailFails() {
        String email = uniqueEmail();
        register(email);
        mail.failSending(true);
        openTheBreaker();

        ApiResponse resend = api().post("/auth/verification/resend", Map.of("email", email));

        assertThat(resend.status()).isEqualTo(202);
    }

    @Test
    void anEmailChangeIsRefusedAsMailUnavailableAndTheEmailStaysTheSame() {
        String email = registerVerifiedAccount();
        String[] tokens = signIn(email);
        mail.failSending(true);
        openTheBreaker();
        String newEmail = uniqueEmail();

        ApiResponse change = api().post(
                        "/users/@me/email-change",
                        Map.of("newEmail", newEmail, "currentPassword", PASSWORD),
                        tokens[0]);

        assertThat(change.status()).isEqualTo(503);
        assertThat(change.text("code")).isEqualTo("mail_unavailable");
        assertThat(change.header("Retry-After")).isNotNull();
        assertThat(mail.countFor(newEmail)).isZero();
        assertThat(signIn(email)).isNotEmpty();
    }

    @Test
    void sendingResumesAfterTheProviderRecoversWithoutARestart() throws InterruptedException {
        String email = uniqueEmail();
        register(email);
        mail.failSending(true);
        openTheBreaker();
        mail.failSending(false);
        Thread.sleep(3500);

        ApiResponse resend = api().post("/auth/verification/resend", Map.of("email", email));

        assertThat(resend.status()).isEqualTo(202);
        String token = mail.latestTokenFor(email).orElseThrow();
        assertThat(api().get("/auth/verification/confirm?token=" + token, null).status())
                .isEqualTo(200);
    }
}
