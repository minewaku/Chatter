package com.minewaku.chatter.identityaccess.user.internal.model;

import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
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

    /**
     * The password a caller presents to confirm an action. A real password always satisfies the password policy, so
     * input that does not is simply a wrong password, not a malformed request.
     */
    public static PlainPassword presented(String raw) {
        try {
            return new PlainPassword(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
    }

    public char[] characters() {
        return Arrays.copyOf(value, value.length);
    }
}
