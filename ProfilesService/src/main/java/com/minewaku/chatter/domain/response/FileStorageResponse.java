package com.minewaku.chatter.domain.response;

import java.net.URI;

import com.minewaku.chatter.domain.value.id.StorageKey;

import io.micrometer.common.lang.NonNull;

public record FileStorageResponse(
    @NonNull StorageKey key,
    @NonNull URI uri,
    @NonNull String format,
    long sizeInBytes) {
}
