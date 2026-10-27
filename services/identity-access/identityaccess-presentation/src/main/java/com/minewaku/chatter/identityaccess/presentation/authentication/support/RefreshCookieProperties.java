package com.minewaku.chatter.identityaccess.presentation.authentication.support;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How the refresh credential cookie is issued. {@code path} scopes it to the auth endpoints so the browser never sends
 * it elsewhere; {@code secure} is only switched off for plain-HTTP local development; {@code maxAge} should match
 * {@code identity-access.session.lifetime}, which the shipped configuration does by reference.
 */
@ConfigurationProperties("identity-access.web.refresh-cookie")
public record RefreshCookieProperties(String name, String path, Boolean secure, Duration maxAge) {

    public RefreshCookieProperties {
        if (name == null || name.isBlank()) {
            name = "refresh_token";
        }
        if (path == null || path.isBlank()) {
            path = "/api/v1/auth";
        }
        if (secure == null) {
            secure = Boolean.TRUE;
        }
        if (maxAge == null) {
            maxAge = Duration.ofDays(30);
        }
        if (maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("identity-access.web.refresh-cookie.max-age must be positive");
        }
    }
}
