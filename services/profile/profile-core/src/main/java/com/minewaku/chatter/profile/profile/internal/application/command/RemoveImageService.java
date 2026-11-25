package com.minewaku.chatter.profile.profile.internal.application.command;

import com.minewaku.chatter.profile.profile.api.command.RemoveImageUseCase;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import com.minewaku.chatter.profile.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profile.profile.internal.port.repository.PendingUploadRepository;
import com.minewaku.chatter.profile.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Empties an image slot. The pointer change and the ledger row for the old key are one transaction; deleting the key
 * comes after and is best-effort, because the ledger row guarantees the sweeper will.
 */
@Service
public class RemoveImageService implements RemoveImageUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemoveImageService.class);

    private final ImageStorage storage;
    private final ProfileRepository profiles;
    private final PendingUploadRepository pending;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public RemoveImageService(
            ImageStorage storage,
            ProfileRepository profiles,
            PendingUploadRepository pending,
            Clock clock,
            TransactionTemplate transaction) {
        this.storage = storage;
        this.profiles = profiles;
        this.pending = pending;
        this.clock = clock;
        this.transaction = transaction;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        ImageSlot slot = ImageSlot.valueOf(command.slot());
        Instant now = clock.instant();

        Optional<OwnedImage> removed = transaction.execute(status -> profiles.clearImage(userId, slot, now).map(id -> {
            OwnedImage image = new OwnedImage(userId, slot, id);
            pending.discard(image, now);
            return image;
        }));

        removed.ifPresent(image -> {
            try {
                storage.delete(image);
                pending.delete(image.imageId());
            } catch (RuntimeException e) {
                log.warn("image delete postponed to the sweeper userId={} slot={}", userId.value(), slot, e);
            }
        });
        return new Result(removed.isPresent());
    }
}
