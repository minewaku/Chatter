package com.minewaku.chatter.profilenext.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token settings. The public key is not here: the application module reads it from Vault, and {@code keys} only says
 * where. The audience is not checked on incoming tokens.
 */
@ConfigurationProperties("profile-next.security.jwt")
public record JwtProperties(String issuer, Keys keys) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "identity-access";
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
