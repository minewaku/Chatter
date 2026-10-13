package com.minewaku.chatter.identityaccess.authentication.internal.port.repository;

import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

public interface AuthenticationSessionRepository {

    /** Atomically stores the session and only a digest of the raw refresh credential. */
    void create(AuthenticationSession session, String rawRefreshToken);

    /** Resolves a presented credential by digest; retained expired or revoked records are returned too. */
    Optional<AuthenticationSession> findByPresentedToken(String rawRefreshToken);

    Optional<AuthenticationSession> findBySessionId(String sessionId);

    /** Sets lastUsedAt only when the record is unrevoked and now is before expiry. */
    boolean touchIfUsable(String sessionId, Instant now);

    /** Sets revokedAt once for an owned, retained, unrevoked record. */
    boolean revokeBySessionId(UserId userId, String sessionId, Instant now);

    /** Revokes all other retained, unrevoked sessions of the user; returns the number changed. */
    int revokeOtherSessions(UserId userId, String currentSessionId, Instant now);

    /** Revokes all retained, unrevoked sessions of the user; returns the number changed. */
    int revokeAllByUserId(UserId userId, Instant now);
}
