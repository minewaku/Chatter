package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

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
    private final List<ApiResponse> refreshIssuingResponses = new ArrayList<>();

    private ApiResponse record(ApiResponse response) {
        responses.add(response);
        return response;
    }

    private ApiResponse recordIssuing(ApiResponse response) {
        responses.add(response);
        refreshIssuingResponses.add(response);
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
                recordIssuing(api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD_SENTINEL)));
        assertThat(signedIn.status()).isEqualTo(200);
        String accessToken = signedIn.text("accessToken");
        String refreshToken = signedIn.text("refreshToken");
        String sid = claim(accessToken, "sid");

        ApiResponse renewed = recordIssuing(api().post("/auth/refresh", Map.of("refreshToken", refreshToken)));
        String renewedRefresh = renewed.text("refreshToken");
        record(api().post("/auth/refresh", Map.of("refreshToken", refreshToken)));
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
        record(api().post("/auth/logout", Map.of("refreshToken", renewedRefresh)));
        record(api().post("/auth/verification/resend", Map.of("email", email)));

        String logs = output.getAll();
        for (String secret : List.of(
                PASSWORD_SENTINEL,
                NEW_PASSWORD_SENTINEL,
                WRONG_PASSWORD_SENTINEL,
                WEAK_PASSWORD_SENTINEL,
                verificationToken,
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
                    verificationToken)) {
                assertThat(response.body()).doesNotContain(secret);
            }
            if (!refreshIssuingResponses.contains(response)) {
                assertThat(response.body()).doesNotContain(refreshToken).doesNotContain(renewedRefresh);
            }
        }
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
