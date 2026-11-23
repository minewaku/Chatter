package com.minewaku.chatter.profilenext.profile.internal.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The identity of one uploaded image. Ids come from a time-ordered generator (UUIDv7), so comparing two of them tells
 * which was created later. The high bits of a UUIDv7 hold a millisecond timestamp far below the sign bit, so the
 * signed comparison of {@link UUID} agrees with time order.
 */
public record ImageId(UUID value) implements Comparable<ImageId> {

    private static final int UUID_TEXT_LENGTH = 36;

    public ImageId {
        Objects.requireNonNull(value, "image id must not be null");
    }

    /** For text that did not come from a client: anything but a UUID is a bug, so it is rejected. */
    public static ImageId of(String text) {
        return tryParse(text).orElseThrow(() -> new IllegalArgumentException("image id must be a UUID"));
    }

    /** For text that came from a client: anything but the text form of a UUID is an ordinary "no", not an error. */
    public static Optional<ImageId> tryParse(String text) {
        if (text == null || text.length() != UUID_TEXT_LENGTH) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ImageId(UUID.fromString(text)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** True when this image was created after {@code other}. A repeat of the same id is not newer. */
    public boolean isNewerThan(ImageId other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(ImageId other) {
        return value.compareTo(other.value);
    }
}
