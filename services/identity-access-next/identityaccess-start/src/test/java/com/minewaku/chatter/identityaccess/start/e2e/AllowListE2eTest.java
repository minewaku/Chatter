package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.security.SecurityProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The public set is exactly the six credential-free operations plus documentation; everything else is protected. */
class AllowListE2eTest extends E2eSupport {

    private static final Set<String> EXPECTED_PUBLIC_PATTERNS = Set.of(
            "/api/v*/auth/register",
            "/api/v*/auth/authenticate",
            "/api/v*/auth/refresh",
            "/api/v*/auth/logout",
            "/api/v*/auth/verification/resend",
            "/api/v*/auth/verification/confirm",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**");

    /** Method, path, body for each protected operation. */
    private static final List<String[]> PROTECTED = List.of(
            new String[] {"GET", "/sessions", null},
            new String[] {"POST", "/sessions/some-session/revoke", "{\"currentPassword\":\"x\"}"},
            new String[] {"POST", "/sessions/revoke-others", "{\"currentPassword\":\"x\"}"},
            new String[] {"PUT", "/users/@me/password", "{\"currentPassword\":\"x\",\"newPassword\":\"y\"}"},
            new String[] {"DELETE", "/users/@me", null});

    @Autowired
    SecurityProperties securityProperties;

    private ApiResponse call(String[] operation, String accessToken) {
        return switch (operation[0]) {
            case "GET" -> api().get(operation[1], accessToken);
            case "DELETE" -> api().delete(operation[1], accessToken);
            case "PUT" -> api().put(operation[1], Map.of("currentPassword", "x", "newPassword", "y"), accessToken);
            default -> api().post(operation[1], Map.of("currentPassword", "x"), accessToken);
        };
    }

    @Test
    void theConfiguredPublicPatternsAreExactlyTheDocumentedSet() {
        assertThat(securityProperties.publicEndpoints()).containsExactlyInAnyOrderElementsOf(EXPECTED_PUBLIC_PATTERNS);
    }

    @Test
    void everyProtectedOperationRejectsEveryKindOfBadCredentialInTheStandardShape() {
        String[] valid = signIn(registerVerifiedAccount());
        String tampered = tamper(valid[0]);
        long userId = Long.parseLong(claim(valid[0], "sub"));
        String sid = claim(valid[0], "sid");
        List<String> badCredentials = new java.util.ArrayList<>();
        badCredentials.add(null);
        badCredentials.add("not-a-jwt");
        badCredentials.add(TestKeys.expiredToken(userId, sid));
        badCredentials.add(tampered);
        badCredentials.add(TestKeys.foreignKeyToken(userId, sid));
        badCredentials.add(TestKeys.wrongIssuerToken(userId, sid));

        for (String[] operation : PROTECTED) {
            for (String credential : badCredentials) {
                ApiResponse response = call(operation, credential);
                String context = operation[0] + " " + operation[1] + " with "
                        + (credential == null ? "none" : credential.substring(0, Math.min(12, credential.length())));
                assertThat(response.status()).as(context).isEqualTo(401);
                assertThat(response.text("code")).as(context).isEqualTo("unauthenticated");
                assertThat(response.header("WWW-Authenticate")).as(context).isEqualTo("Bearer");
            }
        }
        // the genuine credential was never affected
        assertThat(api().get("/sessions", valid[0]).status()).isEqualTo(200);
    }

    @Test
    void publicOperationsIgnoreAnInvalidCredentialAndAnswerOnTheirOwnTerms() {
        ApiResponse register = api().post("/auth/register", Map.of("email", "bad"), "garbage");
        ApiResponse authenticate = api().post("/auth/authenticate", Map.of("email", "bad"), "garbage");
        ApiResponse refresh = api().post("/auth/refresh", Map.of(), "garbage");
        ApiResponse logout = api().post("/auth/logout", Map.of(), "garbage");
        ApiResponse resend = api().post("/auth/verification/resend", Map.of("email", "bad"), "garbage");
        ApiResponse confirm = api().get("/auth/verification/confirm?token=x", "garbage");

        assertThat(register.status()).isEqualTo(400);
        assertThat(register.text("code")).isEqualTo("validation_failed");
        assertThat(authenticate.status()).isEqualTo(400);
        assertThat(authenticate.text("code")).isEqualTo("validation_failed");
        assertThat(refresh.status()).isEqualTo(401);
        assertThat(refresh.text("code")).isEqualTo("authentication_failed");
        assertThat(logout.status()).isEqualTo(204);
        assertThat(resend.status()).isEqualTo(400);
        assertThat(resend.text("code")).isEqualTo("validation_failed");
        assertThat(confirm.status()).isEqualTo(400);
        assertThat(confirm.text("code")).isEqualTo("invalid_or_expired_token");
    }

    private static String tamper(String jwt) {
        String[] parts = jwt.split("\\.");
        char first = parts[1].charAt(5);
        return parts[0] + "." + parts[1].substring(0, 5) + (first == 'A' ? 'B' : 'A') + parts[1].substring(6) + "."
                + parts[2];
    }
}
