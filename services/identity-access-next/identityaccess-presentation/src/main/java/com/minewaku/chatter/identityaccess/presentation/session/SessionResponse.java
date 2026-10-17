package com.minewaku.chatter.identityaccess.presentation.session;

import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import java.time.Instant;

/** One retained session of the caller. Carries no refresh credential, digest, or account identifier. */
public record SessionResponse(
        String sessionId,
        String deviceInfo,
        Instant issuedAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant revokedAt) {

    static SessionResponse from(FindSessionsQuery.SessionView view) {
        return new SessionResponse(
                view.sessionId(),
                view.deviceInfo(),
                view.issuedAt(),
                view.expiresAt(),
                view.lastUsedAt(),
                view.revokedAt());
    }
}
