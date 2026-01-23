package com.minewaku.chatter.adapter.db.redis.dto;

import java.time.Duration;
import java.time.Instant;

import lombok.NonNull;

public record RefreshTokenDto(
        @NonNull String token,
        @NonNull Duration duration,
        @NonNull Instant issuedAt,
        @NonNull Instant expiresAt,
        @NonNull Long userId,
        String replacedBy,
        Boolean revoked,
        Instant revokedAt) {
}
