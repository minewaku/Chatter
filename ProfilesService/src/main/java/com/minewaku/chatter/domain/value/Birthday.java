package com.minewaku.chatter.domain.value;

import java.time.LocalDate;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record Birthday(@NonNull LocalDate value) {

    private static final int MAX_AGE = 150;

    public Birthday {

        LocalDate now = LocalDate.now();
        if (value.isAfter(now)) {
            throw new DomainValidationException("Birthday value cannot be in the future");
        }

        if (value.isBefore(now.minusYears(MAX_AGE))) {
            throw new DomainValidationException("Age seems invalid (max " + MAX_AGE + " years)");
        }
    }

    public static Birthday of(LocalDate value) {
        return value == null ? null : new Birthday(value);
    }
}
