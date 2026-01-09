package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.minewaku.chatter.domain.exception.DomainValidationException;

public record Username(String value) {

    private static final String REGEX = "^(?!.*\\.\\.)[a-z0-9_](?:[a-z0-9_.]{0,30}[a-z0-9_])?$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public Username {
        if (value == null) {
            throw new DomainValidationException("Username cannot be null");
        }

        value = value.trim().toLowerCase();
        if (value.isBlank()) {
            throw new DomainValidationException("Username cannot be blank");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainValidationException(
                "Invalid username. Must be 1-32 chars, lowercase letters, digits, '_' or '.', cannot start/end with '.' or contain '..'. Value: " + value
            );
        }
    }
}