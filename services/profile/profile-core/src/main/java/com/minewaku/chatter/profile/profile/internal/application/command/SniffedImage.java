package com.minewaku.chatter.profile.profile.internal.application.command;

import com.minewaku.chatter.profile.profile.internal.model.ImageDimensions;
import java.util.Objects;

/** What the bytes of a stored file really are: read from the file itself, never from what a client claimed. */
public record SniffedImage(String format, String contentType, ImageDimensions dimensions) {

    public SniffedImage {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(dimensions, "dimensions");
    }
}
