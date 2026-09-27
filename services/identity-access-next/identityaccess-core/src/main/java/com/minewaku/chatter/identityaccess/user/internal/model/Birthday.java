package com.minewaku.chatter.identityaccess.user.internal.model;

import java.time.LocalDate;
import java.time.Period;

public record Birthday(LocalDate value) {

    public Birthday {
        if (value == null) {
            throw new IllegalArgumentException("Birthday is required");
        }
    }

    public boolean isValidOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Reference date is required");
        }

        return !value.isAfter(date) && Period.between(value, date).getYears() <= 150;
    }

    public int ageOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Reference date is required");
        }

        return Period.between(value, date).getYears();
    }
}
