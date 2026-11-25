package com.minewaku.chatter.profile.presentation.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** The caller identity taken only from a validated access token: the account in {@code sub}. Never request input. */
public record AccessPrincipal(long userId) {

    public static AccessPrincipal from(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Invalid subject");
        }
        return new AccessPrincipal(userId);
    }
}
