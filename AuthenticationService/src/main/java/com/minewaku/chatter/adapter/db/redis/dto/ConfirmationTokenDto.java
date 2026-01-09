package com.minewaku.chatter.adapter.db.redis.dto;

import java.time.Duration;
import java.time.Instant;

import lombok.NonNull;

public record ConfirmationTokenDto(
        @NonNull String token,
        @NonNull Long userId,
        @NonNull String email,
        @NonNull Duration duration,
        @NonNull Instant createdAt,
        @NonNull Instant expiresAt,
        Instant confirmedAt) {
}
