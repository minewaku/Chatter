package com.minewaku.chatter.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class RefreshToken {

    @NonNull
    private final String token;

    @NonNull
    private final Duration duration;

    @NonNull
    private final Instant issuedAt;

    @NonNull
    private final Instant expiresAt;

    @NonNull
    private final UserId userId;

    private boolean revoked = false;

    @NonNull
    private Instant revokedAt;

    @NonNull
    private String replacedBy;

    // Private constructor
    private RefreshToken(
            @NonNull String token,
            @NonNull Duration duration,
            @NonNull Instant issuedAt,
            @NonNull Instant expiresAt,
            @NonNull UserId userId,
            String replacedBy,
            boolean revoked,
            Instant revokedAt) {

        this.token = token;
        this.duration = duration;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    public static RefreshToken reconstitute(
            @NonNull String token,
            @NonNull Duration duration,
            @NonNull Instant issuedAt,
            @NonNull Instant expiresAt,
            @NonNull UserId userId,
            String replacedBy,
            boolean revoked,
            Instant revokedAt) {

        return new RefreshToken(token, duration, issuedAt, expiresAt, userId, replacedBy, revoked, revokedAt);
    }

    // Static factory for loading existing data
    public static RefreshToken createNew(
            @NonNull String token,
            Duration duration,
            @NonNull UserId userId) {

        Instant now = Instant.now();
        Duration dur = Objects.isNull(duration) ? Duration.ofMinutes(15L) : duration;

        RefreshToken refreshToken = new RefreshToken(token, dur, now, now.plus(dur), userId, null, false, null);
        return refreshToken;
    }

    public void replace(RefreshToken replacedBy) {
        revoke();
        if (token.equals(replacedBy.getToken())) {
            throw new BusinessRuleViolationException("New token cannot reuse old token value");
        }
        this.replacedBy = replacedBy.getToken();
    }

    public void revoke() {
        if (revoked) {
            throw new BusinessRuleViolationException("Token already revoked at " + revokedAt);
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new BusinessRuleViolationException("Cannot revoke an expired token");
        }
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RefreshToken))
            return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
