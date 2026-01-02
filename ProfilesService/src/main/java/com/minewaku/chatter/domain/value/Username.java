package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class Username {

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^(?!.*\\.\\.)[a-z0-9_](?:[a-z0-9_.]{0,30}[a-z0-9_])?$"
    );

    @Getter
    @NonNull
    private final String value;

    @JsonCreator
    public Username(@JsonProperty("value") String value) {

        if (value == null) {
            throw new DomainValidationException("Username cannot be null");
        }

        String processedValue = value.trim().toLowerCase();
        if (processedValue.isBlank()) {
            throw new DomainValidationException("Username cannot be blank");
        }

        if (!USERNAME_PATTERN.matcher(processedValue).matches()) {
            throw new DomainValidationException(
                "Invalid username. Must be 1-32 chars, lowercase letters, digits, '_' or '.', cannot start/end with '.' or contain '..'. Value: " + processedValue
            );
        }

        this.value = processedValue;
    }
}
