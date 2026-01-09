package com.minewaku.chatter.domain.value.id;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record StorageKey(@NonNull String value) {
    public StorageKey {
        
        value = value.trim();
        if (value.isBlank()) {
            throw new DomainValidationException("Storage key cannot be empty or blank");
        }
    }

    public static StorageKey of(String value) {
        return value == null ? null : new StorageKey(value);
    }
}
