package com.minewaku.chatter.identityaccess.presentation.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * The caller identity taken only from a validated access credential: the account in {@code sub} and the session in
 * {@code sid}. Never built from request input.
 */
public record AccessPrincipal(long userId, String sessionId) {

    public static AccessPrincipal from(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Invalid subject");
        }
        String sessionId = jwt.getClaimAsString("sid");
        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidBearerTokenException("Missing session");
        }
        return new AccessPrincipal(userId, sessionId);
    }
}
