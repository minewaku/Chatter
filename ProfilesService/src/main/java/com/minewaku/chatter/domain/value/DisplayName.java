package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DisplayName {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 32;
    private static final Pattern VALID_CHARACTERS_PATTERN = Pattern.compile("^[^\\n\\r\\t]+$");

    @Getter
    private final String value;

    @JsonCreator
    public DisplayName(@JsonProperty("value") String value) {
        String trimmedValue = (value == null) ? "" : value.trim();

        if (trimmedValue.isBlank()) {
            throw new DomainValidationException("Display name cannot be empty or blank");
        }

        if (trimmedValue.length() < MIN_LENGTH || trimmedValue.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                String.format("Display name must be between %d and %d characters", MIN_LENGTH, MAX_LENGTH)
            );
        }

        if (!VALID_CHARACTERS_PATTERN.matcher(trimmedValue).matches()) {
            throw new DomainValidationException("Display name cannot contain newlines, tabs, or control characters");
        }

        this.value = trimmedValue;
    }
}