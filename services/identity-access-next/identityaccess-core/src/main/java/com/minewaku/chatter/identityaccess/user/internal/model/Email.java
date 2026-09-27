package com.minewaku.chatter.identityaccess.user.internal.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9+_.-]+@[a-z0-9.-]+$");

    public Email {
        if (value == null) {
            throw new IllegalArgumentException("Email is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()
                || value.length() > 320
                || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        int atIndex = value.lastIndexOf('@');
        int domainLength = value.length() - atIndex - 1;
        if (atIndex < 1 || atIndex > 64 || domainLength < 3 || domainLength > 255) {
            throw new IllegalArgumentException("Invalid email length");
        }
    }
}
