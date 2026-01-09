package com.minewaku.chatter.domain.value;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record Bio(@NonNull String value) {
    private static final int MAX_LENGTH = 190;

    public Bio {
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                "Bio is too long. Max length is " + MAX_LENGTH + " characters."
            );
        }
    }

    public static Bio of(String value) {
        return value == null ? null : new Bio(value);
    }
}
