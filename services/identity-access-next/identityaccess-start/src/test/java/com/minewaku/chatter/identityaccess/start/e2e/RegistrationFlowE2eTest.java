package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RegistrationFlowE2eTest extends E2eSupport {

    private static Map<String, Object> registration(String email, String username) {
        return Map.of("email", email, "username", username, "birthday", "1999-01-01", "password", PASSWORD);
    }

    @Test
    void registerVerifyAndSignInEndToEnd() {
        String email = uniqueEmail();
        String username = uniqueUsername();

        ApiResponse registered = api().post("/auth/register", registration(email, username));
        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.json().get("userId").asLong()).isPositive();
        assertThat(registered.body()).doesNotContain(PASSWORD);

        ApiResponse pending = api().post("/auth/register", registration(email, username));
        assertThat(pending.status()).isEqualTo(409);
        assertThat(pending.text("code")).isEqualTo("verification_pending");
        assertThat(mail.countFor(email)).isEqualTo(1);

        ApiResponse beforeVerification = api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD));
        assertThat(beforeVerification.status()).isEqualTo(401);
        assertThat(beforeVerification.text("code")).isEqualTo("authentication_failed");

        String token = mail.latestTokenFor(email).orElseThrow();
        ApiResponse confirmed = api().get("/auth/verification/confirm?token=" + token, null);
        assertThat(confirmed.status()).isEqualTo(200);
        assertThat(confirmed.text("status")).isEqualTo("verified");
        ApiResponse again = api().get("/auth/verification/confirm?token=" + token, null);
        assertThat(again.status()).isEqualTo(200);
        assertThat(again.text("status")).isEqualTo("already_verified");

        ApiResponse signedIn = api().post(
                        "/auth/authenticate",
                        ApiClient.credentials(email, PASSWORD),
                        null,
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
        assertThat(signedIn.status()).isEqualTo(200);
        assertThat(signedIn.json().size()).isEqualTo(2);
        assertThat(signedIn.text("accessToken")).isNotBlank();
        assertThat(signedIn.text("refreshToken")).isNotBlank();
        assertThat(signedIn.header("Set-Cookie")).isNull();

        ApiResponse protectedCall = api().get("/sessions", signedIn.text("accessToken"));
        assertThat(protectedCall.status()).isNotEqualTo(401);

        ApiResponse duplicate = api().post("/auth/register", registration(email, uniqueUsername()));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.text("code")).isEqualTo("account_already_exists");
    }

    @Test
    void unknownEmailWrongPasswordAndInactiveAccountAreIndistinguishable() {
        String verified = registerVerifiedAccount();
        String pendingEmail = uniqueEmail();
        api().post("/auth/register", registration(pendingEmail, uniqueUsername()));

        ApiResponse unknown = api().post("/auth/authenticate", ApiClient.credentials(uniqueEmail(), PASSWORD));
        ApiResponse wrongPassword = api().post("/auth/authenticate", ApiClient.credentials(verified, "Wrong-pass1!"));
        ApiResponse inactive = api().post("/auth/authenticate", ApiClient.credentials(pendingEmail, PASSWORD));

        for (ApiResponse response : new ApiResponse[] {unknown, wrongPassword, inactive}) {
            assertThat(response.status()).isEqualTo(401);
            assertThat(response.text("code")).isEqualTo("authentication_failed");
            assertThat(response.text("message")).isEqualTo(unknown.text("message"));
        }
    }

    @Test
    void verificationRequestDoesNotRevealWhetherTheAddressIsEligible() {
        String pendingEmail = uniqueEmail();
        api().post("/auth/register", registration(pendingEmail, uniqueUsername()));
        String verified = registerVerifiedAccount();

        ApiResponse eligible = api().post("/auth/verification/resend", Map.of("email", pendingEmail));
        ApiResponse ineligible = api().post("/auth/verification/resend", Map.of("email", verified));
        ApiResponse unknown = api().post("/auth/verification/resend", Map.of("email", uniqueEmail()));

        for (ApiResponse response : new ApiResponse[] {eligible, ineligible, unknown}) {
            assertThat(response.status()).isEqualTo(202);
            assertThat(response.body()).isEmpty();
        }
    }

    @Test
    void invalidTokensAreRejectedInTheStandardShape() {
        ApiResponse unknown = api().get("/auth/verification/confirm?token=does-not-exist", null);
        ApiResponse missing = api().get("/auth/verification/confirm", null);

        for (ApiResponse response : new ApiResponse[] {unknown, missing}) {
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.text("code")).isEqualTo("invalid_or_expired_token");
        }
    }
}
