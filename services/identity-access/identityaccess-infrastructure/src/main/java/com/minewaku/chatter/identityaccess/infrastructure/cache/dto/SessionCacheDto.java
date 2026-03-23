package com.minewaku.chatter.identityaccess.infrastructure.cache.dto;

import java.time.Instant;

import lombok.NonNull;

public record SessionCacheDto(
    @NonNull String sessionId,
    @NonNull Long userId,
    @NonNull DeviceInfoDto deviceInfo,
    @NonNull Integer generation,
    @NonNull Instant issuedAt,
    @NonNull Instant expiresAt,
    @NonNull Instant lastRefreshedAt,
    @NonNull Boolean revoked,
    Instant revokedAt
) {
    public record DeviceInfoDto(
        String ipAddress
    ) {}
}
