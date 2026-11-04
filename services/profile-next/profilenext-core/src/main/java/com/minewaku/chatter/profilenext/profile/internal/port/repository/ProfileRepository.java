package com.minewaku.chatter.profilenext.profile.internal.port.repository;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
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
     * Puts {@code image} in the slot if its upload id is newer than the stored one, creating the row if needed. When
     * applied, {@code previous} is the image that was replaced (null if the slot was empty). When not applied, the
     * confirmation was a repeat or an older one and {@code previous} is the image currently stored.
     */
    Replacement replaceImage(UserId userId, ImageSlot slot, StoredImage image, Instant now);

    /** Removes the user's row and returns the images it held; empty when there was no row. */
    Optional<DeletedProfile> deleteByUserId(UserId userId);

    record Replacement(boolean applied, StoredImage previous) {}

    record DeletedProfile(StoredImage avatar, StoredImage banner) {}
}
