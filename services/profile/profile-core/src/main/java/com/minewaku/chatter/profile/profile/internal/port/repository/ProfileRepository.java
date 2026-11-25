package com.minewaku.chatter.profile.profile.internal.port.repository;

import com.minewaku.chatter.profile.profile.internal.model.Bio;
import com.minewaku.chatter.profile.profile.internal.model.DisplayName;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.Profile;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

public interface ProfileRepository {

    /**
     * Saves display name and/or bio in one atomic statement, creating the row on the first save. A null part is left
     * unchanged. Returns true when a row was written; false when the values equal what is stored.
     */
    boolean save(UserId userId, DisplayName displayName, Bio bio, Instant now);

    /** The user's profile, or {@link Profile#defaultsFor} when there is no row. Does not lock. */
    Profile getOrDefault(UserId userId);

    /**
     * Makes {@code imageId} the slot's image, creating the row when there is none, and returns the image it replaced
     * (empty when the slot was empty). Concurrent calls for one user serialise on the row. Must be called inside a
     * transaction.
     */
    Optional<ImageId> replaceImage(UserId userId, ImageSlot slot, ImageId imageId, Instant now);

    /** Empties the slot and returns the image it held; empty (and no change) when it was already empty. */
    Optional<ImageId> clearImage(UserId userId, ImageSlot slot, Instant now);

    /** Removes the user's row and returns the images it held; empty when there was no row. */
    Optional<DeletedProfile> deleteByUserId(UserId userId);

    /** The images a deleted row held; either may be null. */
    record DeletedProfile(ImageId avatar, ImageId banner) {}
}
