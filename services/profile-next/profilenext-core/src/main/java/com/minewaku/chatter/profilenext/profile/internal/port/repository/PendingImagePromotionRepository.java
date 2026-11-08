package com.minewaku.chatter.profilenext.profile.internal.port.repository;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The work queue of images waiting to move from temporary to permanent storage. Written in the same transaction as
 * the profile change that applied the image. An image with a row here is "not promoted" for readers.
 */
public interface PendingImagePromotionRepository {

    /** Queues an image, due now; a second call for the same image id is ignored. */
    void enqueue(OwnedImage image, Instant now);

    /**
     * Takes up to {@code limit} due, unreported rows (oldest first) and hides them from other workers for
     * {@code lease}. Two callers never receive the same row at the same time.
     */
    List<PendingPromotion> takeDue(Instant now, int limit, Duration lease);

    /** Deletes the row of an image that was moved. From now on readers see the image as promoted. */
    void markPromoted(ImageId imageId);

    /** Records a failed attempt; {@code reportedAt} is set (non-null) once the horizon is exhausted. */
    void markFailed(ImageId imageId, int attempts, Instant nextAttemptAt, Instant reportedAt);

    /** Deletes the row of an image if there is one (used when the image is replaced); no-op otherwise. */
    void remove(ImageId imageId);

    /** Deletes every row of a user (account deletion). */
    void deleteByUserId(UserId userId);

    /** An image waiting to be promoted: how often it failed and when it was queued. */
    record PendingPromotion(OwnedImage image, int attempts, Instant createdAt) {}
}
