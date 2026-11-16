package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImagePath;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.StoredObject;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingUploadRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Commits an upload: makes the file the user sent straight to storage the profile's image for a slot.
 *
 * <p>Everything before the final transaction only <em>reads</em> storage, so any failure there changes nothing and the
 * client can repeat the call. The transaction is the commit point: it removes the ledger row (the key is now current),
 * flips the profile pointer, and records the replaced key for deletion, all or nothing. Deleting the replaced key
 * comes after, best-effort, because its ledger row already guarantees the sweeper will do it. A refusal records its
 * intent in the ledger <em>before</em> deleting the file, for the same reason.
 *
 * <p>No storage call runs inside a transaction.
 */
@Service
public class CommitImageUploadService implements CommitImageUploadUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommitImageUploadService.class);

    private final ImageStorage storage;
    private final ProfileRepository profiles;
    private final PendingUploadRepository pending;
    private final ImagePolicy policy;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public CommitImageUploadService(
            ImageStorage storage,
            ProfileRepository profiles,
            PendingUploadRepository pending,
            ImagePolicy policy,
            Clock clock,
            TransactionTemplate transaction) {
        this.storage = storage;
        this.profiles = profiles;
        this.pending = pending;
        this.policy = policy;
        this.clock = clock;
        this.transaction = transaction;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        ImageSlot slot = ImageSlot.valueOf(command.slot());
        ImageId imageId = parse(command.imageId());
        Instant now = clock.instant();

        // 1. a repeat of a commit that already went through
        if (profiles.currentImage(userId, slot).filter(imageId::equals).isPresent()) {
            return new Result(Outcome.ALREADY_APPLIED, text(imageId), ImagePath.of(userId, slot, imageId));
        }
        PendingUpload upload = pending.findPending(userId, imageId)
                .filter(found -> found.image().slot() == slot)
                .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_NOT_FOUND));
        if (upload.isExpiredAt(now)) {
            throw new ImageUploadRejectedException(Reason.UPLOAD_EXPIRED);
        }
        OwnedImage image = upload.image();

        // 2. what the storage holds
        StoredObject stored =
                storage.stat(image).orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_INCOMPLETE));
        policy.checkStored(slot, stored).ifPresent(reason -> {
            throw refuse(image, reason, now);
        });

        // 3. what the file's own bytes say, unless the provider already validated them
        if (!storage.validatesOnUpload()) {
            SniffedImage sniffed = ImageSniffer.sniff(storage.readHeader(image, policy.headerBytes()), stored.sizeBytes())
                    .orElseThrow(() -> refuse(image, Reason.UNREADABLE_IMAGE, now));
            policy.checkContent(slot, stored, sniffed).ifPresent(reason -> {
                throw refuse(image, reason, now);
            });
        }

        // 4. the commit point
        Optional<ImageId> previous;
        try {
            previous = transaction.execute(status -> {
                pending.lockPending(userId, imageId)
                        .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_NOT_FOUND));
                pending.delete(imageId);
                Optional<ImageId> replaced = profiles.replaceImage(userId, slot, imageId, now);
                replaced.ifPresent(old -> pending.discard(new OwnedImage(userId, slot, old), now));
                return replaced;
            });
        } catch (ImageUploadRejectedException e) {
            // two commits of one ticket raced and the other one won: this one is the repeat it would have been
            if (e.reason() == Reason.UPLOAD_NOT_FOUND
                    && profiles.currentImage(userId, slot).filter(imageId::equals).isPresent()) {
                return new Result(Outcome.ALREADY_APPLIED, text(imageId), ImagePath.of(userId, slot, imageId));
            }
            throw e;
        }

        // 5. the fast path for the replaced key; the sweeper is the safety net
        previous.ifPresent(old -> deleteNow(new OwnedImage(userId, slot, old)));
        log.info("image committed userId={} slot={} imageId={} replaced={}", userId.value(), slot, text(imageId), previous.isPresent());
        return new Result(Outcome.APPLIED, text(imageId), ImagePath.of(userId, slot, imageId));
    }

    /** Records that the key is to be deleted, tries to delete it now, and returns the refusal for the caller to throw. */
    private ImageUploadRejectedException refuse(OwnedImage image, Reason reason, Instant now) {
        pending.discard(image, now);
        deleteNow(image);
        log.info(
                "image commit refused rule={} userId={} slot={} imageId={}",
                reason.name().toLowerCase(java.util.Locale.ROOT),
                image.userId().value(),
                image.slot(),
                text(image.imageId()));
        return new ImageUploadRejectedException(reason);
    }

    /** Deletes the key and its ledger row; on any failure the row stays and the sweeper retries. */
    private void deleteNow(OwnedImage image) {
        try {
            storage.delete(image);
            pending.delete(image.imageId());
        } catch (RuntimeException e) {
            log.warn(
                    "image delete postponed to the sweeper userId={} slot={} imageId={}",
                    image.userId().value(),
                    image.slot(),
                    text(image.imageId()),
                    e);
        }
    }

    private static String text(ImageId id) {
        return id.value().toString();
    }

    /** Whatever does not parse is "not found"; the client's text is never echoed or logged. */
    private static ImageId parse(String text) {
        try {
            return ImageId.of(text);
        } catch (IllegalArgumentException e) {
            throw new ImageUploadRejectedException(Reason.UPLOAD_NOT_FOUND);
        }
    }
}
