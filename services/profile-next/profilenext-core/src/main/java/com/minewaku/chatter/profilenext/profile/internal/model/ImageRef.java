package com.minewaku.chatter.profilenext.profile.internal.model;

import java.util.Objects;

/**
 * What a reader needs to find an image: its id, and whether it has been moved to permanent storage yet. Read model
 * only; the write model carries no promotion state.
 */
public record ImageRef(ImageId id, boolean promoted) {

    public ImageRef {
        Objects.requireNonNull(id, "id");
    }
}
