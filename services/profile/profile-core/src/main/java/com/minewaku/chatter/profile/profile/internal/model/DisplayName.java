package com.minewaku.chatter.profile.profile.internal.model;

import com.minewaku.chatter.profile.profile.api.exception.InvalidProfileValueException;

/**
 * A display name: 1 to 32 user-perceived characters after trimming, no control characters. Built from user input and
 * when the write side loads a profile to edit it; the read side uses plain values. Tightening the rules therefore
 * needs existing rows migrated first.
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
        String trimmed = value.strip();
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
