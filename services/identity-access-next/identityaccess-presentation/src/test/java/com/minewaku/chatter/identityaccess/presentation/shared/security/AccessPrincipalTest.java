package com.minewaku.chatter.identityaccess.presentation.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class AccessPrincipalTest {

    private static Jwt jwt(String subject, String sid) {
        Jwt.Builder builder = Jwt.withTokenValue("t")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (subject != null) {
            builder.subject(subject);
        }
        if (sid != null) {
            builder.claim("sid", sid);
        }
        return builder.build();
    }

    @Test
    void readsAccountAndSessionFromTheCredential() {
        AccessPrincipal principal = AccessPrincipal.from(jwt("1234", "session-9"));

        assertThat(principal.userId()).isEqualTo(1234L);
        assertThat(principal.sessionId()).isEqualTo("session-9");
    }

    @Test
    void nonNumericOrMissingSubjectIsAnInvalidCredential() {
        assertThatThrownBy(() -> AccessPrincipal.from(jwt("not-a-number", "s")))
                .isInstanceOf(InvalidBearerTokenException.class);
        assertThatThrownBy(() -> AccessPrincipal.from(jwt(null, "s"))).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void missingOrBlankSessionClaimIsAnInvalidCredential() {
        assertThatThrownBy(() -> AccessPrincipal.from(jwt("1", null))).isInstanceOf(InvalidBearerTokenException.class);
        assertThatThrownBy(() -> AccessPrincipal.from(jwt("1", "  "))).isInstanceOf(InvalidBearerTokenException.class);
    }
}
