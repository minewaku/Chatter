package com.minewaku.chatter.profile.profile.internal.port.repository;

import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * The upload ledger: every key the service authorized or retired and that is not the current image of a profile. Rows are
 * written in the same transaction as the change that creates them, and always before the storage call they announce.
 */
public interface PendingUploadRepository {

    /** The pending (not discarded) ticket of this user with this image id, if any. */
    Optional<PendingUpload> findPending(UserId userId, ImageId imageId);

    /**
     * Like {@link #findPending(UserId, ImageId)} but holds an exclusive lock on the row until the surrounding transaction
     * ends. Must be called inside a transaction.
     */
    Optional<PendingUpload> lockPending(UserId userId, ImageId imageId);

    /** Adds a pending ticket. A user may hold any number of them, also for one slot. */
    void insert(PendingUpload upload);

    /**
     * Makes the image a key to delete once {@code deleteAfter} has passed: updates its row when there is one, adds one
     * otherwise.
     */
    void discard(OwnedImage image, Instant deleteAfter);

    /** Turns every pending ticket of the user into a discard row, keeping its {@code deleteAfter}. */
    void discardAllPending(UserId userId);

    /**
     * Turns every pending ticket of the user for this slot into a discard row, keeping its {@code deleteAfter}: an
     * upload of such a ticket may still arrive, so its key is swept only after the grace.
     */
    void discardPending(UserId userId, ImageSlot slot);

    /** Removes the row of an image: it became current, or its key is gone. No-op when there is none. */
    void delete(ImageId imageId);

    /**
     * Takes the oldest row whose {@code deleteAfter} has passed and that no other worker holds, locking it until the
     * surrounding transaction ends. Must be called inside a transaction.
     */
    Optional<PendingUpload> lockNextDue(Instant now);

    /** Moves a row's {@code deleteAfter} forward, after a failed delete, so it does not block the rows behind it. */
    void postpone(ImageId imageId, Instant deleteAfter);
}
