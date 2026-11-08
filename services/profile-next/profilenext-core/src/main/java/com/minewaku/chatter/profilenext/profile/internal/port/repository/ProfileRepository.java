package com.minewaku.chatter.profilenext.profile.internal.port.repository;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

public interface ProfileRepository {

    /**
     * Saves display name and/or bio in one atomic statement, creating the row on the first save. A null part is left
     * unchanged. Returns true when a row was written; false when the values equal what is stored.
     */
    boolean applyEdit(UserId userId, DisplayName displayName, Bio bio, Instant now);

    /**
     * Loads the user's profile and holds an exclusive lock on it until the surrounding transaction ends, so concurrent
     * image confirmations for one user serialise. Empty when there is no row. Must be called inside a transaction.
     */
    Optional<Profile> findForUpdate(UserId userId);

    /** Sets one slot (and the update time) on an existing row. Not called when there is no row. */
    void setImage(UserId userId, ImageSlot slot, ImageId imageId, Instant now);

    /**
     * Empties a slot, but only if it currently holds {@code imageId}. Returns whether it did; false (and no change)
     * when the slot holds something else or there is no row.
     */
    boolean clearImageIf(UserId userId, ImageSlot slot, ImageId imageId, Instant now);

    /** Removes the user's row and returns the images it held; empty when there was no row. */
    Optional<DeletedProfile> deleteByUserId(UserId userId);

    /** The images a deleted row held; either may be null. */
    record DeletedProfile(ImageId avatar, ImageId banner) {}
}
