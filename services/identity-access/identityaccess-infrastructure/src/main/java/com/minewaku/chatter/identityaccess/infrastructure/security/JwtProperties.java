package com.minewaku.chatter.identityaccess.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Token settings. The signing keys are not here: the application module reads them from Vault, and {@code keys} only
 * says where. The audience is written into issued tokens and is not checked on incoming ones.
 */
@Validated
@ConfigurationProperties("identity-access.security.jwt")
public record JwtProperties(String issuer, String audience, Duration accessTokenTtl, Keys keys) {

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
        if (keys == null) {
            keys = new Keys(null, null);
        }
    }

    /** Where the key pair lives in Vault's KV version 2 store. */
    public record Keys(String backend, String path) {

        public Keys {
            if (backend == null || backend.isBlank()) {
                backend = "secret";
            }
            if (path == null || path.isBlank()) {
                path = "common/jwt/rs256";
            }
        }
    }
}
