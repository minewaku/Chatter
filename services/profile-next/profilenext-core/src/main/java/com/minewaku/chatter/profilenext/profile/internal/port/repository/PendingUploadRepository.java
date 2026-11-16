package com.minewaku.chatter.profilenext.profile.internal.port.repository;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * The upload ledger: every key the service authorized or retired and that is not the current image of a profile. Rows are
 * written in the same transaction as the change that creates them, and always before the storage call they announce.
 */
public interface PendingUploadRepository {

    /** The one pending ticket of this user and slot, if any (expired or not). */
    Optional<PendingUpload> findPending(UserId userId, ImageSlot slot);

    /** The pending (not discarded) ticket of this user with this image id, if any. */
    Optional<PendingUpload> findPending(UserId userId, ImageId imageId);

    /**
     * Like {@link #findPending(UserId, ImageId)} but holds an exclusive lock on the row until the surrounding transaction
     * ends. Must be called inside a transaction.
     */
    Optional<PendingUpload> lockPending(UserId userId, ImageId imageId);

    /**
     * Adds a pending ticket. Returns false, and changes nothing, when the user and slot already have a pending ticket
     * (a concurrent request won); the caller then reads that one.
     */
    boolean insertPending(PendingUpload upload);

    /**
     * Makes the image a key to delete once {@code deleteAfter} has passed: updates its row when there is one, adds one
     * otherwise.
     */
    void discard(OwnedImage image, Instant deleteAfter);

    /** Turns every pending ticket of the user into a discard row, keeping its {@code deleteAfter}. */
    void discardAllPending(UserId userId);

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
