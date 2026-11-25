package com.minewaku.chatter.profile.profile.internal.model;

import java.util.Objects;

/** One image of one user in one slot: the three values that always travel together across the ports. */
public record OwnedImage(UserId userId, ImageSlot slot, ImageId imageId) {

    public OwnedImage {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(imageId, "imageId");
    }
}
