package com.minewaku.chatter.identityaccess.infrastructure.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code publicEndpoints} are Ant-style patterns reachable without a credential; everything else needs a valid
 * one. {@code cors.allowedOrigins} is empty by default, which denies cross-origin browser calls.
 */
@ConfigurationProperties("identity-access.security")
public record SecurityProperties(List<String> publicEndpoints, Cors cors) {

    public static final List<String> DEFAULT_PUBLIC_ENDPOINTS = List.of(
            "/api/v*/auth/register",
            "/api/v*/auth/authenticate",
            "/api/v*/auth/refresh",
            "/api/v*/auth/logout",
            "/api/v*/auth/verification/resend",
            "/api/v*/auth/verification/confirm",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**");

    public SecurityProperties {
        if (publicEndpoints == null || publicEndpoints.isEmpty()) {
            publicEndpoints = DEFAULT_PUBLIC_ENDPOINTS;
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
