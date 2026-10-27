package com.minewaku.chatter.identityaccess.presentation.authentication.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Writes and reads the refresh credential cookie: {@code HttpOnly}, {@code SameSite=Strict}, scoped to the auth path. */
@Component
@RequiredArgsConstructor
public class RefreshCookies {

    private final RefreshCookieProperties properties;

    /** The {@code Set-Cookie} value that stores {@code refreshToken} for the session lifetime. */
    public String issue(String refreshToken) {
        return build(refreshToken, properties.maxAge());
    }

    /** The {@code Set-Cookie} value that makes the browser drop the cookie. */
    public String clear() {
        return build("", Duration.ZERO);
    }

    /** The presented credential, or {@code null} when the request carries no such cookie. */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.name().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite("Strict")
                .path(properties.path())
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
