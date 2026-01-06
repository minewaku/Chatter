package com.minewaku.chatter.domain.response;

import java.net.URI;

import io.micrometer.common.lang.NonNull;

public record FileStorageResponse(
    @NonNull String fileKey,
    @NonNull URI fileUrl,
    @NonNull String format,
    long sizeInBytes) {
}
