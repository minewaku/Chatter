package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileDataUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImagePromotionRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes a deleted account's profile row, queues its images for removal and drops its pending promotions, in one
 * transaction. Naturally idempotent: with no row
 * there is nothing to delete and nothing to queue, and that is success. The stored images are deleted later by the
 * removal worker, so a storage outage never fails the notice.
 */
@Service
@Transactional
public class DeleteProfileDataService implements DeleteProfileDataUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteProfileDataService.class);

    private final ProfileRepository profiles;
    private final PendingImageRemovalRepository removals;
    private final PendingImagePromotionRepository promotions;
    private final Clock clock;

    public DeleteProfileDataService(
            ProfileRepository profiles,
            PendingImageRemovalRepository removals,
            PendingImagePromotionRepository promotions,
            Clock clock) {
        this.profiles = profiles;
        this.removals = removals;
        this.promotions = promotions;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        profiles.deleteByUserId(userId).ifPresent(deleted -> {
            queue(userId, deleted.avatar(), ImageSlot.AVATAR);
            queue(userId, deleted.banner(), ImageSlot.BANNER);
        });
        promotions.deleteByUserId(userId);
        log.info("account data deleted eventId={} userId={}", command.eventId(), userId.value());
        return new Result();
    }

    private void queue(UserId userId, ImageId imageId, ImageSlot slot) {
        if (imageId != null) {
            removals.enqueue(new OwnedImage(userId, slot, imageId), clock.instant());
        }
    }
}
