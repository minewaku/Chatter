package com.minewaku.chatter.identityaccess.application.port.outbound.query.model;

import java.time.Instant;

import com.minewaku.chatter.identityaccess.application.shared.DeviceInfoDto;

import lombok.NonNull;

public record SessionReadModel(
    @NonNull String sessionId,
    @NonNull Long userId,
    @NonNull DeviceInfoDto deviceInfo,
    @NonNull Instant issuedAt,
    @NonNull Instant expiresAt,
    Instant lastRefreshedAt
) {
}
