package com.minewaku.chatter.identityaccess.user.internal.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * A new email address awaiting proof of ownership. The account keeps its current email until the change is
 * confirmed with the raw token, which only ever travels to the new address.
 */
@Getter
public final class PendingEmailChange {

    private final String rawToken;
    private final UserId userId;
    private final Email newEmail;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant confirmedAt;

    private PendingEmailChange(
            String rawToken, UserId userId, Email newEmail, Instant createdAt, Instant expiresAt, Instant confirmedAt) {
        this.rawToken = requireText(rawToken, "raw token is required");
        this.userId = Objects.requireNonNull(userId, "user id is required");
        this.newEmail = Objects.requireNonNull(newEmail, "new email is required");
        this.createdAt = Objects.requireNonNull(createdAt, "created time is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiry time is required");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiry time must be after creation time");
        }
        if (confirmedAt != null && (confirmedAt.isBefore(createdAt) || !confirmedAt.isBefore(expiresAt))) {
            throw new IllegalArgumentException("confirmed time must be within the token lifetime");
        }
        this.confirmedAt = confirmedAt;
    }

    public static PendingEmailChange issue(
            String rawToken, UserId userId, Email newEmail, Clock clock, Duration lifetime) {
        Objects.requireNonNull(clock, "clock is required");
        requirePositive(lifetime);
        Instant createdAt = clock.instant();
        return new PendingEmailChange(rawToken, userId, newEmail, createdAt, createdAt.plus(lifetime), null);
    }

    public static PendingEmailChange reconstitute(
            String rawToken, UserId userId, Email newEmail, Instant createdAt, Instant expiresAt, Instant confirmedAt) {
        return new PendingEmailChange(rawToken, userId, newEmail, createdAt, expiresAt, confirmedAt);
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

    public boolean isConfirmed() {
        return confirmedAt != null;
    }

    public boolean confirm(Instant now) {
        Objects.requireNonNull(now, "current time is required");
        if (isConfirmed() || isExpired(now)) {
            return false;
        }
        confirmedAt = now;
        return true;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }
}
