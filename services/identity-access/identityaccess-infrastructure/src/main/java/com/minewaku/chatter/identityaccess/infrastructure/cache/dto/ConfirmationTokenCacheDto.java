package com.minewaku.chatter.identityaccess.infrastructure.cache.dto;

import java.time.Duration;
import java.time.Instant;

import lombok.NonNull;

public record ConfirmationTokenCacheDto(
        @NonNull String token,
        @NonNull Long userId,
        @NonNull String email,
        @NonNull Duration duration,
        @NonNull Instant createdAt,
        @NonNull Instant expiresAt,
        Instant confirmedAt) {
}
