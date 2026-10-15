package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication.JwtAccessTokenIssuer;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtDecoderTest {

    private final KeyPair pair = RsaKeyFixtures.newKeyPair();
    private final JwtProperties properties = properties(pair, "identity-access", "chatter");
    private final JwtDecoder decoder = new JwtDecoderConfiguration().jwtDecoder(properties);

    private static JwtProperties properties(KeyPair pair, String issuer, String audience) {
        return new JwtProperties(
                RsaKeyFixtures.privatePem(pair),
                RsaKeyFixtures.publicPem(pair),
                issuer,
                audience,
                Duration.ofMinutes(15));
    }

    private static String issue(JwtProperties properties, Instant at) {
        Clock clock = Clock.fixed(at, ZoneOffset.UTC);
        return new JwtAccessTokenIssuer(properties, clock).issue(new UserId(77L), new Email("alice@example.com"));
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void aFreshlyIssuedCredentialDecodes() {
        Jwt jwt = decoder.decode(issue(properties, now()));

        assertThat(jwt.getSubject()).isEqualTo("77");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@example.com");
    }

    @Test
    void anExpiredCredentialIsRejected() {
        String token = issue(properties, now().minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTamperedCredentialIsRejected() {
        String token = issue(properties, now());
        String[] parts = token.split("\\.");
        char first = parts[1].charAt(5);
        String tamperedPayload = parts[1].substring(0, 5) + (first == 'A' ? 'B' : 'A') + parts[1].substring(6);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialSignedByAForeignKeyIsRejected() {
        JwtProperties foreign = properties(RsaKeyFixtures.newKeyPair(), "identity-access", "chatter");

        assertThatThrownBy(() -> decoder.decode(issue(foreign, now()))).isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialWithTheWrongIssuerIsRejected() {
        JwtProperties wrongIssuer = properties(pair, "someone-else", "chatter");

        assertThatThrownBy(() -> decoder.decode(issue(wrongIssuer, now()))).isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialWithTheWrongAudienceIsRejected() {
        JwtProperties wrongAudience = properties(pair, "identity-access", "another-app");

        assertThatThrownBy(() -> decoder.decode(issue(wrongAudience, now()))).isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialIssuedByAPreviousKeyIsRejectedAfterKeyReplacement() {
        String oldCredential = issue(properties, now());
        JwtDecoder replaced = new JwtDecoderConfiguration()
                .jwtDecoder(properties(RsaKeyFixtures.newKeyPair(), "identity-access", "chatter"));

        assertThatThrownBy(() -> replaced.decode(oldCredential)).isInstanceOf(JwtException.class);
    }
}
