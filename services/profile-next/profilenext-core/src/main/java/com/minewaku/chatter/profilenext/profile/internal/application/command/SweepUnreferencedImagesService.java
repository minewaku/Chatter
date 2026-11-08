package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.SweepUnreferencedImagesUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageRateLimitedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredImageInfo;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredImagePage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Removes permanent provider files that no profile references: files whose queued removal was given up on, and strays
 * left by races. Files younger than the grace period, referenced files, and files with a live queue row are left alone.
 * The storage port lists permanent images only, so temporary uploads are never touched here.
 */
@Service
public class SweepUnreferencedImagesService implements SweepUnreferencedImagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(SweepUnreferencedImagesService.class);

    private final ImageStorage storage;
    private final ProfileQueryRepository profiles;
    private final PendingImageRemovalRepository removals;
    private final SweepPolicy policy;
    private final Clock clock;

    public SweepUnreferencedImagesService(
            ImageStorage storage,
            ProfileQueryRepository profiles,
            PendingImageRemovalRepository removals,
            SweepPolicy policy,
            Clock clock) {
        this.storage = storage;
        this.profiles = profiles;
        this.removals = removals;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        Instant cutoff = clock.instant().minus(policy.gracePeriod());
        int removed = 0;
        String cursor = null;
        try {
            do {
                StoredImagePage page = storage.list(cursor);
                removed += sweep(page.items(), cutoff);
                cursor = page.nextCursor();
            } while (cursor != null);
        } catch (ImageStorageRateLimitedException e) {
            log.info("image sweep stopped early: the provider asked to back off, removed={}", removed);
        }
        return new Result(removed);
    }

    private int sweep(List<StoredImageInfo> items, Instant cutoff) {
        List<OwnedImage> old = items.stream()
                .filter(item -> item.createdAt().isBefore(cutoff))
                .map(StoredImageInfo::image)
                .toList();
        if (old.isEmpty()) {
            return 0;
        }
        Set<ImageId> referenced = profiles.findReferenced(old);
        Set<ImageId> active = removals.findActive(old.stream().map(OwnedImage::imageId).toList());
        int removed = 0;
        for (OwnedImage image : old) {
            if (referenced.contains(image.imageId()) || active.contains(image.imageId())) {
                continue;
            }
            try {
                storage.remove(image);
                removals.clear(image.imageId());
                removed++;
            } catch (ImageStorageRateLimitedException e) {
                throw e;
            } catch (ImageStorageUnavailableException e) {
                log.warn(
                        "image sweep could not remove a file, will retry next run imageId={} userId={}",
                        image.imageId().value(),
                        image.userId().value());
            }
        }
        return removed;
    }
}
