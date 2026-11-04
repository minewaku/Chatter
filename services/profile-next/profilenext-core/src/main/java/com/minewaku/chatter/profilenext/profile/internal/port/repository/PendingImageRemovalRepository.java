package com.minewaku.chatter.profilenext.profile.internal.port.repository;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingRemoval;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** The work queue of provider files to delete. Written in the same transaction as the change that obsoletes a file. */
public interface PendingImageRemovalRepository {

    /** Queues a file; a second call for the same public id is ignored. */
    void enqueue(String publicId, ImageSlot slot, Instant now);

    /**
     * Takes up to {@code limit} due, unreported rows (oldest first) and hides them from other workers for
     * {@code lease}. Two callers never receive the same row at the same time.
     */
    List<PendingRemoval> takeDue(Instant now, int limit, Duration lease);

    void markRemoved(String publicId);

    /** Records a failed attempt; {@code reportedAt} is set (non-null) once the retry limit is exhausted. */
    void markFailed(String publicId, int attempts, Instant nextAttemptAt, Instant reportedAt);

    /** Of the given ids, those that have a queue row which is not yet reported. */
    Set<String> findActivePublicIds(Collection<String> publicIds);

    /** Drops the row for a file the sweep removed. */
    void clear(String publicId);
}
