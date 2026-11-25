package com.minewaku.chatter.profile.profile.internal.model;

import java.time.Instant;
import java.util.Optional;

/**
 * What the service knows about one user. {@code displayName}, {@code avatar} and {@code banner} are null until the user
 * sets them; {@code createdAt} and {@code updatedAt} are null while no row exists, which is how a profile that was never
 * saved is told apart from a saved one. An image is only its identity: where it lives and how it is served are derived
 * elsewhere from the owner, the slot and that identity.
 */
public record Profile(
        UserId userId,
        DisplayName displayName,
        Bio bio,
        ImageId avatar,
        ImageId banner,
        Instant createdAt,
        Instant updatedAt) {

    /** The profile of a user who never saved anything: no name, empty bio, no images, no timestamps. */
    public static Profile defaultsFor(UserId userId) {
        return new Profile(userId, null, Bio.of(""), null, null, null, null);
    }

    /** The slot that holds this image, if either does. */
    public Optional<ImageSlot> slotHolding(ImageId imageId) {
        if (imageId.equals(avatar)) {
            return Optional.of(ImageSlot.AVATAR);
        }
        return imageId.equals(banner) ? Optional.of(ImageSlot.BANNER) : Optional.empty();
    }

    /** The image held in {@code slot}, or null when the slot is empty. */
    public ImageId imageIn(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? avatar : banner;
    }
}
