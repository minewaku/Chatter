package com.minewaku.chatter.profilenext.profile.internal.model;

import com.minewaku.chatter.profilenext.profile.api.exception.InvalidProfileValueException;

/**
 * A display name: 1 to 32 user-perceived characters after trimming, no control characters. Built only from user input,
 * never from stored values, so a rule change cannot make old rows unreadable.
 */
public record DisplayName(String value) {

    public static final String FIELD = "displayName";
    public static final int MAX_LENGTH = 32;
    private static final String LENGTH_MESSAGE = "Display name must be 1 to 32 characters.";

    public DisplayName {
        if (value == null) {
            throw new InvalidProfileValueException(FIELD, "display_name_blank", LENGTH_MESSAGE);
        }
        if (value.length() > PerceivedText.MAX_RAW_UNITS) {
            throw new InvalidProfileValueException(FIELD, "display_name_length", LENGTH_MESSAGE);
        }
        String trimmed = PerceivedText.trim(value);
        if (trimmed.isEmpty()) {
            throw new InvalidProfileValueException(FIELD, "display_name_blank", LENGTH_MESSAGE);
        }
        if (PerceivedText.hasControlCharacters(trimmed)) {
            throw new InvalidProfileValueException(
                    FIELD, "display_name_control_characters", "Display name must not contain control characters.");
        }
        if (PerceivedText.count(trimmed) > MAX_LENGTH) {
            throw new InvalidProfileValueException(FIELD, "display_name_length", LENGTH_MESSAGE);
        }
        value = trimmed;
    }

    public static DisplayName of(String raw) {
        return new DisplayName(raw);
    }
}
