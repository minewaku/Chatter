package com.minewaku.chatter.profilenext.infrastructure.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code publicEndpoints} are path patterns reachable without a token; everything else needs a valid one.
 * {@code cors.allowedOrigins} is empty by default, which denies cross-origin browser calls.
 */
@ConfigurationProperties("profile-next.security")
public record SecurityProperties(List<String> publicEndpoints, Cors cors) {

    public SecurityProperties {
        if (publicEndpoints == null) {
            publicEndpoints = List.of();
        }
        if (cors == null) {
            cors = new Cors(List.of());
        }
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            if (allowedOrigins == null) {
                allowedOrigins = List.of();
            }
        }
    }
}
