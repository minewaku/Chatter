package com.minewaku.chatter.identityaccess.authentication.internal.model;

import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/** One bounded, revocable sign-in for one account and one device label. Holds no refresh credential. */
@Getter
public final class AuthenticationSession {

    private final String sessionId;
    private final UserId userId;
    private final String deviceInfo;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant lastUsedAt;
    private Instant revokedAt;

    private AuthenticationSession(
            String sessionId,
            UserId userId,
            String deviceInfo,
            Instant issuedAt,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant revokedAt) {
        if (deviceInfo == null || deviceInfo.isBlank() || deviceInfo.length() > 200) {
            throw new IllegalArgumentException("Device label must be nonblank and at most 200 characters");
        }
        this.sessionId = Objects.requireNonNull(sessionId, "session id is required");
        this.userId = Objects.requireNonNull(userId, "user id is required");
        this.deviceInfo = deviceInfo;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issued time is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiry time is required");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Expiry must be after issue time");
        }
        if (lastUsedAt != null && lastUsedAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("Last use cannot precede issue time");
        }
        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
    }

    public static AuthenticationSession issue(
            String sessionId, UserId userId, String deviceInfo, Instant issuedAt, Duration lifetime) {
        Objects.requireNonNull(lifetime, "lifetime is required");
        if (lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("Session lifetime must be positive");
        }
        return new AuthenticationSession(sessionId, userId, deviceInfo, issuedAt, issuedAt.plus(lifetime), null, null);
    }

    public static AuthenticationSession reconstitute(
            String sessionId,
            UserId userId,
            String deviceInfo,
            Instant issuedAt,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant revokedAt) {
        return new AuthenticationSession(sessionId, userId, deviceInfo, issuedAt, expiresAt, lastUsedAt, revokedAt);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && !isExpired(now);
    }

    /** Records a successful renewal; the last-use time never moves backwards. */
    public void markUsed(Instant now) {
        if (!isUsable(now)) {
            throw new IllegalStateException("Only a usable session can be used");
        }
        if (lastUsedAt == null || now.isAfter(lastUsedAt)) {
            lastUsedAt = now;
        }
    }

    /** Sets revokedAt once; returns false when the session was already revoked. */
    public boolean revoke(Instant now) {
        if (revokedAt != null) {
            return false;
        }
        revokedAt = now;
        return true;
    }
}
