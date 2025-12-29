package com.minewaku.chatter.adapter.db.redis.dto;

import java.time.Duration;
import java.time.Instant;

public record RefreshTokenDto(
        String token,
        Duration duration,
        Instant issuedAt,
        Instant expiresAt,
        Long userId,
        String replacedBy,
        Boolean revoked,
        Instant revokedAt) {
}
