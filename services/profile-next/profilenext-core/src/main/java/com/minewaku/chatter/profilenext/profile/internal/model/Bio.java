package com.minewaku.chatter.profilenext.profile.internal.model;

import com.minewaku.chatter.profilenext.profile.api.exception.InvalidProfileValueException;

/** A bio: at most 190 user-perceived characters after trimming, may be empty, no control characters. */
public record Bio(String value) {

    public static final String FIELD = "bio";
    public static final int MAX_LENGTH = 190;
    private static final String LENGTH_MESSAGE = "Bio must be at most 190 characters.";

    public Bio {
        if (value == null) {
            value = "";
        }
        if (value.length() > PerceivedText.MAX_RAW_UNITS) {
            throw new InvalidProfileValueException(FIELD, "bio_length", LENGTH_MESSAGE);
        }
        String trimmed = PerceivedText.trim(value);
        if (PerceivedText.hasControlCharacters(trimmed)) {
            throw new InvalidProfileValueException(
                    FIELD, "bio_control_characters", "Bio must not contain control characters.");
        }
        if (PerceivedText.count(trimmed) > MAX_LENGTH) {
            throw new InvalidProfileValueException(FIELD, "bio_length", LENGTH_MESSAGE);
        }
        value = trimmed;
    }

    public static Bio of(String raw) {
        return new Bio(raw);
    }
}
