package com.minewaku.chatter.profilenext.profile.internal.port.repository;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The work queue of provider files to delete. Written in the same transaction as the change that obsoletes a file.
 * Each row carries the owner, because the profile row may be gone by the time the worker runs.
 */
public interface PendingImageRemovalRepository {

    /** Queues an image; a second call for the same image id is ignored. */
    void enqueue(OwnedImage image, Instant now);

    /**
     * Takes up to {@code limit} due, unreported rows (oldest first) and hides them from other workers for
     * {@code lease}. Two callers never receive the same row at the same time.
     */
    List<PendingRemoval> takeDue(Instant now, int limit, Duration lease);

    void markRemoved(ImageId imageId);

    /** Records a failed attempt; {@code reportedAt} is set (non-null) once the retry limit is exhausted. */
    void markFailed(ImageId imageId, int attempts, Instant nextAttemptAt, Instant reportedAt);

    /** Of the given ids, those that have a queue row which is not yet reported. */
    Set<ImageId> findActive(Collection<ImageId> imageIds);

    /** Drops the row for an image the sweep removed. */
    void clear(ImageId imageId);

    /** A provider file waiting to be deleted, with the number of failed attempts so far. */
    record PendingRemoval(OwnedImage image, int attempts) {}
}
