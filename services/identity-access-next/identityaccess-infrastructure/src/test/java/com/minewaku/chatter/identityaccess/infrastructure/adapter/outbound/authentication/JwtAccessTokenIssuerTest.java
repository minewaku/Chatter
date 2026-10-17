package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@ExtendWith(OutputCaptureExtension.class)
class JwtAccessTokenIssuerTest {

    private final KeyPair pair = RsaKeyFixtures.newKeyPair();
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtProperties properties = new JwtProperties(
            RsaKeyFixtures.privatePem(pair),
            RsaKeyFixtures.publicPem(pair),
            "test-issuer",
            "test-audience",
            Duration.ofMinutes(7));

    private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(properties, clock);

    @Test
    void tokenCarriesTheRequiredClaimsAndTheConfiguredLifetime() {
        String token = issuer.issue(new UserId(1234L), new Email("alice@example.com"), "session-1");

        Jwt jwt = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic())
                .build()
                .decode(token);

        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jwt.getSubject()).isEqualTo("1234");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@example.com");
        assertThat(jwt.getClaimAsString("sid")).isEqualTo("session-1");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(jwt.getAudience()).containsExactly("test-audience");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(7)));
    }

    @Test
    void tokenIsRejectedByADecoderWithAnotherKey() {
        String token = issuer.issue(new UserId(1L), new Email("alice@example.com"), "session-1");
        RSAPublicKey foreign = (RSAPublicKey) RsaKeyFixtures.newKeyPair().getPublic();

        assertThatThrownBy(() -> NimbusJwtDecoder.withPublicKey(foreign).build().decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void invalidKeysStopConstruction() {
        JwtProperties broken = new JwtProperties(" ", properties.publicKey(), "i", "a", Duration.ofMinutes(1));

        assertThatThrownBy(() -> new JwtAccessTokenIssuer(broken, clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity-access.security.jwt.private-key");
    }

    @Test
    void neverLogsTheTokenOrThePrivateKey(CapturedOutput output) {
        String token = issuer.issue(new UserId(1L), new Email("alice@example.com"), "session-1");

        assertThat(output.getAll()).doesNotContain(token).doesNotContain("PRIVATE KEY");
    }
}
