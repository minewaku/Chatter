package com.minewaku.chatter.identityaccess.user.internal.model;

import java.util.Arrays;
import java.util.regex.Pattern;

/** Input-only password; never persisted, logged, or returned. */
public final class PlainPassword {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=[^A-Z]*+[A-Z])" + "(?=[^a-z]*+[a-z])" + "(?=\\D*+\\d)" + "(?=[^#?!@$%^&*-]*+[#?!@$%^&*-])" + ".{8,}$");
    private final char[] value;

    public PlainPassword(String raw) {
        if (raw == null || raw.isBlank() || !PASSWORD_PATTERN.matcher(raw).matches()) {
            throw new IllegalArgumentException("Invalid password format");
        }
        value = raw.toCharArray();
    }

    public char[] characters() {
        return Arrays.copyOf(value, value.length);
    }
}
