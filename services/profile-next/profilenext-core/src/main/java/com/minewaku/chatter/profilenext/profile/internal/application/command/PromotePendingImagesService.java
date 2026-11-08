package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.PromotePendingImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImagePromotionRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImagePromotionRepository.PendingPromotion;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Drains the promotion queue. Deliberately not transactional, like the removal worker: taking rows is one short
 * transaction in the repository (it leases them), each provider call runs outside any transaction, and each outcome is
 * another short one.
 *
 * <p>Giving up (a failure at or after the horizon) first empties the slot, but only if it still holds the image, and
 * only then marks the row reported. The order matters: if the process dies between the two steps the row is still
 * unreported, so the next run repeats both, and the repeat is harmless.
 */
@Service
public class PromotePendingImagesService implements PromotePendingImagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(PromotePendingImagesService.class);

    private final ImageStorage storage;
    private final PendingImagePromotionRepository promotions;
    private final ProfileRepository profiles;
    private final PromotionPolicy policy;
    private final Clock clock;

    public PromotePendingImagesService(
            ImageStorage storage,
            PendingImagePromotionRepository promotions,
            ProfileRepository profiles,
            PromotionPolicy policy,
            Clock clock) {
        this.storage = storage;
        this.promotions = promotions;
        this.profiles = profiles;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        Instant now = clock.instant();
        List<PendingPromotion> due = promotions.takeDue(now, policy.pollBatchSize(), policy.lease());
        int promoted = 0;
        int failed = 0;
        int reported = 0;
        for (PendingPromotion row : due) {
            try {
                storage.promote(row.image());
                promotions.markPromoted(row.image().imageId());
                promoted++;
            } catch (RuntimeException e) {
                failed++;
                if (recordFailure(row, now)) {
                    reported++;
                }
            }
        }
        return new Result(promoted, failed, reported);
    }

    /** Returns true when this failure was final: the slot was cleared (if it still held the image) and the row reported. */
    private boolean recordFailure(PendingPromotion row, Instant now) {
        OwnedImage image = row.image();
        int attempts = row.attempts() + 1;
        if (policy.expired(row.createdAt(), now)) {
            profiles.clearImageIf(image.userId(), image.slot(), image.imageId(), now);
            promotions.markFailed(image.imageId(), attempts, now, now);
            log.error(
                    "image promotion gave up imageId={} userId={} slot={} attempts={}",
                    image.imageId().value(),
                    image.userId().value(),
                    image.slot(),
                    attempts);
            return true;
        }
        promotions.markFailed(image.imageId(), attempts, now.plus(policy.delayBeforeRetry(attempts)), null);
        log.warn(
                "image promotion failed, will retry imageId={} userId={} attempts={}",
                image.imageId().value(),
                image.userId().value(),
                attempts);
        return false;
    }
}
