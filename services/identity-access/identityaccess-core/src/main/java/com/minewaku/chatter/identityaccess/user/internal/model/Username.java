package com.minewaku.chatter.identityaccess.user.internal.model;

import java.util.regex.Pattern;

public record Username(String value) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^(?!.*\\.\\.)\\w(?:[\\w.]{0,30}\\w)?$");

    public Username {
        if (value == null || value.isBlank() || !USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 2-32 characters and may contain letters, digits, '_', or '.'");
        }
    }
}
