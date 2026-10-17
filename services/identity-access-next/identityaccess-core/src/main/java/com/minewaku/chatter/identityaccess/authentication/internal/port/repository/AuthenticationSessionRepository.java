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

    /**
     * Atomically renews a session: only when it is unrevoked, unexpired at {@code now}, and the presented credential
     * belongs to it, the stored credential digest is replaced by the new one's, {@code expiresAt} becomes
     * {@code newExpiresAt}, and {@code lastUsedAt} advances to {@code now}. Returns false and changes nothing
     * otherwise. Of two concurrent calls with the same credential at most one returns true.
     */
    boolean rotate(
            String sessionId, String presentedRefreshToken, String newRefreshToken, Instant now, Instant newExpiresAt);

    /**
     * Sets revokedAt once for an owned, retained, unrevoked record. A session newly revoked by this call, or by
     * either revoke method below, must also make the access credentials issued for it unusable for the access
     * credential lifetime (adapter obligation).
     */
    boolean revokeBySessionId(UserId userId, String sessionId, Instant now);

    /** Revokes all other retained, unrevoked sessions of the user; returns the number changed. */
    int revokeOtherSessions(UserId userId, String currentSessionId, Instant now);

    /** Revokes all retained, unrevoked sessions of the user; returns the number changed. */
    int revokeAllByUserId(UserId userId, Instant now);
}
