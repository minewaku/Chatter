package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImagePromotionRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a provider confirmation once. The owner's profile is locked first, and every decision is made on that locked
 * state, so concurrent confirmations and an account deletion cannot interleave badly:
 *
 * <ul>
 *   <li>a file that breaks the policy is <em>returned</em> as {@code FILE_NOT_ALLOWED} before any lock is taken;
 *   <li>no profile for the user (for example a deleted account) is {@code IGNORED} and nothing is created;
 *   <li>an image newer than the stored one replaces it: the replaced one is queued for removal and its pending
 *       promotion is cancelled, and the new one is queued for promotion;
 *   <li>a repeat or an older image is {@code IGNORED}.
 * </ul>
 *
 * Rejected, stale and abandoned files are never queued for removal here; the provider expires them.
 */
@Service
@Transactional
public class ConfirmImageUploadService implements ConfirmImageUploadUseCase {

    private final ImageStorage storage;
    private final ProfileRepository profiles;
    private final PendingImageRemovalRepository removals;
    private final PendingImagePromotionRepository promotions;
    private final ImagePolicy policy;
    private final Clock clock;

    public ConfirmImageUploadService(
            ImageStorage storage,
            ProfileRepository profiles,
            PendingImageRemovalRepository removals,
            PendingImagePromotionRepository promotions,
            ImagePolicy policy,
            Clock clock) {
        this.storage = storage;
        this.profiles = profiles;
        this.removals = removals;
        this.promotions = promotions;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        Instant now = clock.instant();
        ConfirmedUpload upload = storage.verify(command.rawBody(), command.signature(), command.timestamp(), now)
                .orElseThrow(() -> new UploadConfirmationRejectedException(Reason.INVALID_SIGNATURE));
        if (!policy.permits(upload)) {
            return new Result(Outcome.FILE_NOT_ALLOWED);
        }

        Optional<Profile> locked = profiles.findForUpdate(upload.userId());
        if (locked.isEmpty()) {
            return new Result(Outcome.IGNORED);
        }
        ImageId current = locked.get().imageIn(upload.slot());
        if (current != null && !upload.imageId().isNewerThan(current)) {
            return new Result(Outcome.IGNORED);
        }

        profiles.setImage(upload.userId(), upload.slot(), upload.imageId(), now);
        if (current != null) {
            removals.enqueue(new OwnedImage(upload.userId(), upload.slot(), current), now);
            promotions.remove(current);
        }
        promotions.enqueue(upload.image(), now);
        return new Result(Outcome.APPLIED);
    }
}
