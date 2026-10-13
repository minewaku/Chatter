package com.minewaku.chatter.identityaccess.emailverification.internal.model;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class EmailVerification {

    private final String rawToken;
    private final UserId userId;
    private final Email email;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant verifiedAt;

    private EmailVerification(
            String rawToken, UserId userId, Email email, Instant createdAt, Instant expiresAt, Instant verifiedAt) {
        this.rawToken = requireText(rawToken, "raw token is required");
        this.userId = Objects.requireNonNull(userId, "user id is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.createdAt = Objects.requireNonNull(createdAt, "created time is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiry time is required");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiry time must be after creation time");
        }
        if (verifiedAt != null && (verifiedAt.isBefore(createdAt) || !verifiedAt.isBefore(expiresAt))) {
            throw new IllegalArgumentException("verified time must be within the token lifetime");
        }
        this.verifiedAt = verifiedAt;
    }

    public static EmailVerification issue(String rawToken, UserId userId, Email email, Clock clock, Duration lifetime) {
        Objects.requireNonNull(clock, "clock is required");
        requirePositive(lifetime);
        Instant createdAt = clock.instant();
        return new EmailVerification(rawToken, userId, email, createdAt, createdAt.plus(lifetime), null);
    }

    public static EmailVerification reconstitute(
            String rawToken, UserId userId, Email email, Instant createdAt, Instant expiresAt, Instant verifiedAt) {
        return new EmailVerification(rawToken, userId, email, createdAt, expiresAt, verifiedAt);
    }

    public static void requirePositive(Duration lifetime) {
        Objects.requireNonNull(lifetime, "lifetime is required");
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
    }

    public boolean isExpired(Instant now) {
        return !Objects.requireNonNull(now, "current time is required").isBefore(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean verify(Instant now) {
        Objects.requireNonNull(now, "current time is required");
        if (isVerified() || isExpired(now)) {
            return false;
        }
        verifiedAt = now;
        return true;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }
}
