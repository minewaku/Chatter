package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record DisplayName(@NonNull String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 32;
    private static final String REGEX = "^[^\\n\\r\\t]+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public DisplayName {
        value = value.trim();
        if (value.isBlank()) {
            throw new DomainValidationException("Display name cannot be empty or blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                "Display name must be between %d and %d characters".formatted(MIN_LENGTH, MAX_LENGTH)
            );
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainValidationException("Display name cannot contain newlines, tabs, or control characters");
        }
    }

    public static DisplayName of(String value) {
        return value == null ? null : new DisplayName(value);
    }
}