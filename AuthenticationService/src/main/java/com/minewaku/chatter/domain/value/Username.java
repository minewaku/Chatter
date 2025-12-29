package com.minewaku.chatter.domain.value;

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

    private static final String USERNAME_PATTERN = "^(?!.*\\.\\.)[A-Za-z0-9_](?:[A-Za-z0-9_.]{0,30}[A-Za-z0-9_])?$";

    @Getter
    @NonNull
    private final String value;

    @JsonCreator
    public Username(
            @JsonProperty("value") @NonNull String value) {

        if (value.isBlank()) {
            throw new DomainValidationException("Username cannot be blank");
        }
        if (!value.matches(USERNAME_PATTERN)) {
            throw new DomainValidationException(
                    "Invalid username. Must be 2-32 chars, letters, digits, '_' or '.', cannot start/end with '.' or contain '..': "
                            + value);
        }
        this.value = value;
    }
}
