package com.minewaku.chatter.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class Bio {
    private static final int MAX_LENGTH = 190;

    @Getter
    private final String value;

    @JsonCreator
    public Bio(@JsonProperty("value") String value) {
        String trimmedValue = (value == null) ? "" : value.trim();

        if (trimmedValue.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                "Description is too long. Max length is " + MAX_LENGTH + " characters."
            );
        }

        this.value = trimmedValue;
    }
}
