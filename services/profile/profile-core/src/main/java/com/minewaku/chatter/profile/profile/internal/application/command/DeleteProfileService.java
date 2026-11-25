package com.minewaku.chatter.profile.profile.internal.application.command;

import com.minewaku.chatter.profile.profile.api.command.DeleteProfileUseCase;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import com.minewaku.chatter.profile.profile.internal.port.repository.PendingUploadRepository;
import com.minewaku.chatter.profile.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles an account-deleted notice: removes the profile row, retires both current images and every pending ticket of
 * the user, in one transaction. Retired pending tickets keep their sweep time, so an upload that was already under way
 * is still cleaned up after it lands; and they can no longer be committed. No storage call happens here, so a storage
 * outage never fails the notice, and a repeat of the notice finds nothing left to do.
 */
@Service
@Transactional
public class DeleteProfileService implements DeleteProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteProfileService.class);

    private final ProfileRepository profiles;
    private final PendingUploadRepository pending;
    private final Clock clock;

    public DeleteProfileService(ProfileRepository profiles, PendingUploadRepository pending, Clock clock) {
        this.profiles = profiles;
        this.pending = pending;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        Instant now = clock.instant();
        profiles.deleteByUserId(userId).ifPresent(deleted -> {
            retire(userId, ImageSlot.AVATAR, deleted.avatar(), now);
            retire(userId, ImageSlot.BANNER, deleted.banner(), now);
        });
        pending.discardAllPending(userId);
        log.info("account data deleted eventId={} userId={}", command.eventId(), userId.value());
        return new Result();
    }

    private void retire(UserId userId, ImageSlot slot, ImageId imageId, Instant now) {
        if (imageId != null) {
            pending.discard(new OwnedImage(userId, slot, imageId), now);
        }
    }
}
