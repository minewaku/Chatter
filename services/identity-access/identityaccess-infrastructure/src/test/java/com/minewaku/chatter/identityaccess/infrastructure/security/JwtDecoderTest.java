package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication.JwtAccessTokenIssuer;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtDecoderTest {

    private final KeyPair pair = RsaKeyFixtures.newKeyPair();
    private final JwtProperties properties = properties("identity-access", "chatter");
    private final JwtDecoder decoder = new JwtDecoderConfiguration().jwtDecoder(properties, RsaKeyFixtures.keys(pair));

    private static JwtProperties properties(String issuer, String audience) {
        return new JwtProperties(issuer, audience, Duration.ofMinutes(15), null);
    }

    private static String issue(KeyPair signingPair, JwtProperties properties, Instant at) {
        Clock clock = Clock.fixed(at, ZoneOffset.UTC);
        return new JwtAccessTokenIssuer(RsaKeyFixtures.keys(signingPair), properties, clock)
                .issue(new UserId(77L), new Email("alice@example.com"), "session-1");
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void aFreshlyIssuedCredentialDecodes() {
        Jwt jwt = decoder.decode(issue(pair, properties, now()));

        assertThat(jwt.getSubject()).isEqualTo("77");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@example.com");
    }

    @Test
    void anExpiredCredentialIsRejected() {
        String token = issue(pair, properties, now().minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTamperedCredentialIsRejected() {
        String token = issue(pair, properties, now());
        String[] parts = token.split("\\.");
        char first = parts[1].charAt(5);
        String tamperedPayload = parts[1].substring(0, 5) + (first == 'A' ? 'B' : 'A') + parts[1].substring(6);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialSignedByAForeignKeyIsRejected() {
        KeyPair foreign = RsaKeyFixtures.newKeyPair();

        assertThatThrownBy(() -> decoder.decode(issue(foreign, properties, now())))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialWithTheWrongIssuerIsRejected() {
        JwtProperties wrongIssuer = properties("someone-else", "chatter");

        assertThatThrownBy(() -> decoder.decode(issue(pair, wrongIssuer, now())))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void aCredentialNamingAnotherAudienceIsAccepted() {
        JwtProperties otherAudience = properties("identity-access", "another-app");

        Jwt jwt = decoder.decode(issue(pair, otherAudience, now()));

        assertThat(jwt.getAudience()).containsExactly("another-app");
    }

    @Test
    void aCredentialWithNoAudienceIsAccepted() {
        Instant at = now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("identity-access")
                .subject("77")
                .claim("sid", "session-1")
                .issuedAt(at)
                .expiresAt(at.plus(Duration.ofMinutes(15)))
                .build();
        RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) pair.getPublic())
                .privateKey(pair.getPrivate())
                .build();
        String token = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)))
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getAudience()).isNull();
    }

    @Test
    void theDecoderDoesNotNeedAnAudienceSetting() {
        JwtProperties unset = new JwtProperties(null, null, null, null);
        JwtDecoder bare = new JwtDecoderConfiguration().jwtDecoder(unset, RsaKeyFixtures.keys(pair));

        assertThat(bare.decode(issue(pair, properties("identity-access", "another-app"), now()))
                        .getSubject())
                .isEqualTo("77");
    }

    @Test
    void aCredentialIssuedByAPreviousKeyIsRejectedAfterKeyReplacement() {
        String oldCredential = issue(pair, properties, now());
        JwtDecoder replaced =
                new JwtDecoderConfiguration().jwtDecoder(properties, RsaKeyFixtures.keys(RsaKeyFixtures.newKeyPair()));

        assertThatThrownBy(() -> replaced.decode(oldCredential)).isInstanceOf(JwtException.class);
    }
}
