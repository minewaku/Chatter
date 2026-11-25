package com.minewaku.chatter.profile.profile.internal.model;

/** Width and height of an image in pixels. Used only to check an incoming file against the image policy. */
public record ImageDimensions(int width, int height) {

    public ImageDimensions {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image dimensions must be positive");
        }
    }
}
