package com.minewaku.chatter.adapter.db.redis.dto;

import java.time.Duration;
import java.time.Instant;

public record ConfirmationTokenDto(
        String token,
        Long userId,
        String email,
        Duration duration,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt) {
}
