package com.minewaku.chatter.identityaccess.user.internal.model;

import java.util.regex.Pattern;

public record Username(String value) {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^(?!.*\\.\\.)[A-Za-z0-9_](?:[A-Za-z0-9_.]{0,30}[A-Za-z0-9_])?$");

    public Username {
        if (value == null || value.isBlank() || !USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 2-32 characters and may contain letters, digits, '_', or '.'");
        }
    }
}
