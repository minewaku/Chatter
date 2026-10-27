package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;

/**
 * Runs the whole service with sentinel passwords, a sentinel verification token, and real refresh credentials, then
 * searches every log line and response for them (SC-005).
 */
@ExtendWith(OutputCaptureExtension.class)
class SecretLeakageE2eTest extends E2eSupport {

    private static final String PASSWORD_SENTINEL = "Sentinel-Pw-9!";
    private static final String NEW_PASSWORD_SENTINEL = "Sentinel-New-8@";
    private static final String WRONG_PASSWORD_SENTINEL = "Sentinel-Wrong-7#";
    private static final String WEAK_PASSWORD_SENTINEL = "sentinelweak";

    @Autowired
    Environment environment;

    private final List<ApiResponse> responses = new ArrayList<>();

    private ApiResponse record(ApiResponse response) {
        responses.add(response);
        return response;
    }

    @Test
    void noPasswordTokenOrRefreshCredentialReachesAnyLogLineOrResponse(CapturedOutput output) {
        String email = uniqueEmail();

        record(api().post(
                        "/auth/register",
                        Map.of(
                                "email",
                                email,
                                "username",
                                uniqueUsername(),
                                "birthday",
                                "1999-01-01",
                                "password",
                                WEAK_PASSWORD_SENTINEL)));
        record(api().post(
                        "/auth/register",
                        Map.of(
                                "email",
                                email,
                                "username",
                                uniqueUsername(),
                                "birthday",
                                "1999-01-01",
                                "password",
                                PASSWORD_SENTINEL)));
        record(api().postRaw(
                        "/auth/register", "{\"password\":\"" + PASSWORD_SENTINEL + "\", broken", "application/json"));
        record(api().post("/auth/authenticate", ApiClient.credentials(email, WRONG_PASSWORD_SENTINEL)));

        String verificationToken = mail.latestTokenFor(email).orElseThrow();
        record(api().get("/auth/verification/confirm?token=" + verificationToken, null));
        record(api().get("/auth/verification/confirm?token=" + verificationToken, null));

        ApiResponse signedIn =
                record(api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD_SENTINEL)));
        assertThat(signedIn.status()).isEqualTo(200);
        String accessToken = signedIn.text("accessToken");
        String refreshToken = signedIn.refreshCookie();
        String sid = claim(accessToken, "sid");

        ApiResponse renewed = record(api().refresh(refreshToken));
        String renewedRefresh = renewed.refreshCookie();
        record(api().get("/sessions", accessToken));
        record(api().post(
                        "/sessions/" + sid + "/revoke",
                        Map.of("currentPassword", WRONG_PASSWORD_SENTINEL),
                        accessToken));
        record(api().post("/sessions/revoke-others", Map.of("currentPassword", WRONG_PASSWORD_SENTINEL), accessToken));
        record(api().put(
                        "/users/@me/password",
                        Map.of("currentPassword", PASSWORD_SENTINEL, "newPassword", WEAK_PASSWORD_SENTINEL),
                        accessToken));
        record(api().put(
                        "/users/@me/password",
                        Map.of("currentPassword", WRONG_PASSWORD_SENTINEL, "newPassword", NEW_PASSWORD_SENTINEL),
                        accessToken));
        record(api().post("/users/@me/close", Map.of("currentPassword", WRONG_PASSWORD_SENTINEL), accessToken));
        String changeTarget = uniqueEmail();
        record(api().post(
                        "/users/@me/email-change",
                        Map.of("newEmail", changeTarget, "currentPassword", WRONG_PASSWORD_SENTINEL),
                        accessToken));
        ApiResponse accepted = api().post(
                        "/users/@me/email-change",
                        Map.of("newEmail", changeTarget, "currentPassword", PASSWORD_SENTINEL),
                        accessToken);
        record(accepted);
        String emailChangeToken = mail.latestTokenFor(changeTarget).orElseThrow();
        record(api().get("/users/email-change/confirm?token=" + emailChangeToken, null));
        record(api().get("/users/email-change/confirm?token=" + emailChangeToken, null));
        // replaying the replaced credential ends the session, so it comes after every call that needs the session
        record(api().refresh(refreshToken));
        record(api().logout(renewedRefresh));
        record(api().post("/auth/verification/resend", Map.of("email", email)));

        String logs = output.getAll();
        assertThat(logs.split("reason=refresh-reuse", -1).length - 1)
                .as("exactly one reuse record")
                .isEqualTo(1);
        assertThat(logs).contains("sessionId=" + sid);
        assertThat(logs).doesNotContain(TokenDigest.sha256Hex(refreshToken));
        assertThat(logs).doesNotContain(TokenDigest.sha256Hex(renewedRefresh));
        for (String secret : List.of(
                PASSWORD_SENTINEL,
                NEW_PASSWORD_SENTINEL,
                WRONG_PASSWORD_SENTINEL,
                WEAK_PASSWORD_SENTINEL,
                verificationToken,
                emailChangeToken,
                changeTarget,
                refreshToken,
                renewedRefresh,
                accessToken)) {
            assertThat(logs).as("log output must not contain a secret").doesNotContain(secret);
        }

        for (ApiResponse response : responses) {
            for (String secret : List.of(
                    PASSWORD_SENTINEL,
                    NEW_PASSWORD_SENTINEL,
                    WRONG_PASSWORD_SENTINEL,
                    WEAK_PASSWORD_SENTINEL,
                    verificationToken,
                    emailChangeToken,
                    changeTarget)) {
                assertThat(response.body()).doesNotContain(secret);
            }
            // the refresh credential only ever travels in the Set-Cookie header
            assertThat(response.body()).doesNotContain(refreshToken).doesNotContain(renewedRefresh);
        }
    }

    @Test
    void noSigningKeyMaterialReachesLogsHealthInfoOrErrorResponses(CapturedOutput output) {
        String email = registerVerifiedAccount();
        String[] tokens = signIn(email);
        List<ApiResponse> seen = new ArrayList<>();
        seen.add(api().getRoot("/actuator/health"));
        seen.add(api().getRoot("/actuator/info"));
        seen.add(api().get("/sessions", null));
        seen.add(api().get("/sessions", "not-a-jwt"));
        seen.add(api().get("/sessions", TestKeys.foreignKeyToken(1L, "x")));
        seen.add(api().get("/sessions", TestKeys.expiredToken(1L, "x")));
        seen.add(api().get("/sessions", tokens[0]));

        for (String body : List.of(pemBody(TestKeys.privatePem()), pemBody(TestKeys.publicPem()))) {
            assertThat(output.getAll())
                    .as("log output must not contain key material")
                    .doesNotContain(body);
            for (ApiResponse response : seen) {
                assertThat(response.body()).doesNotContain(body);
            }
        }
        assertThat(output.getAll()).doesNotContain("PRIVATE KEY");
    }

    private static String pemBody(String pem) {
        return pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    }

    @Test
    void theServiceDoesNotEnableRequestOrResponseLogging() {
        assertThat(environment.getProperty("server.tomcat.accesslog.enabled", "false"))
                .isEqualTo("false");
        assertThat(environment.getProperty("spring.mvc.log-request-details", "false"))
                .isEqualTo("false");
        assertThat(environment.getProperty("spring.http.log-request-details", "false"))
                .isEqualTo("false");
        for (String logger : List.of(
                "org.springframework.web",
                "org.springframework.security",
                "org.apache.coyote",
                "org.apache.tomcat",
                "com.minewaku")) {
            String level = environment.getProperty("logging.level." + logger, "INFO");
            assertThat(level).as(logger).isNotIn("DEBUG", "TRACE", "debug", "trace");
        }
    }
}
