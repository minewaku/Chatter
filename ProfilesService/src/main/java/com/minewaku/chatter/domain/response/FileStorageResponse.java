package com.minewaku.chatter.domain.response;

import java.net.URI;

import io.micrometer.common.lang.NonNull;

public record FileStorageResponse(
    @NonNull String key,
    @NonNull URI uri,
    @NonNull String format,
    long sizeInBytes) {
}
