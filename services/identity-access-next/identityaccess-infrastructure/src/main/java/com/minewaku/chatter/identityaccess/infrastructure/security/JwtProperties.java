package com.minewaku.chatter.identityaccess.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("identity-access.security.jwt")
public record JwtProperties(
        String privateKey, String publicKey, String issuer, String audience, Duration accessTokenTtl) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "identity-access";
        }
        if (audience == null || audience.isBlank()) {
            audience = "chatter";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("identity-access.security.jwt.access-token-ttl must be positive");
        }
    }
}
