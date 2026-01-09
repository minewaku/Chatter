package com.minewaku.chatter.domain.value.id;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record UserId(@NonNull Long value) {
    public UserId {
        if (value <= 0) {
            throw new DomainValidationException("UserId value cannot be smaller than 1");
        }
    }

    public static UserId of(Long value) {
        return value == null ? null : new UserId(value);
    }
}
