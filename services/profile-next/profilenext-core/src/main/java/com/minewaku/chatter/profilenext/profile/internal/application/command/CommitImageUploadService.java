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
 * <p>The steps, in order: find the ticket, check the file arrived and is the kind of image the slot allows, then commit.
 * Only what the file's bytes say is judged here; the length the client declared was checked when the ticket was
 * requested and is the storage's to enforce.
 *
 * <p>This service never deletes from storage. The only storage call is the read of the file's first bytes, before the
 * final transaction, so a failure there changes nothing and the client can repeat the call. Every key that stops being
 * wanted, a refused file or the image a commit replaces, is recorded in the ledger as due for deletion and the sweeper
 * deletes it. The transaction is the commit point: it removes the ledger row (the key is now current), retires the
 * user's other tickets for the slot, flips the profile pointer, and records the replaced key for deletion, all or
 * nothing.
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
        ImageId imageId = ImageId.tryParse(command.imageId())
                .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_NOT_FOUND)); // never echo the client's text
        Instant now = clock.instant();

        // 1. a repeat of a commit that already went through: its ticket is gone, the profile holds the image
        Optional<ImageSlot> held = profiles.getOrDefault(userId).slotHolding(imageId);
        if (held.isPresent()) {
            return alreadyApplied(userId, held.get(), imageId);
        }
        PendingUpload upload = pending.findPending(userId, imageId)
                .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_NOT_FOUND));
        if (upload.isExpiredAt(now)) {
            throw new ImageUploadRejectedException(Reason.UPLOAD_EXPIRED);
        }
        OwnedImage image = upload.image();
        ImageSlot slot = image.slot();

        // 2. the file must have arrived; only its first bytes are read
        byte[] firstBytes = storage.readFirstBytes(image, policy.inspectBytes())
                .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_INCOMPLETE));

        // 3. what the file's own bytes say it is, whatever the provider did on upload. The length is not judged
        // here: the ticket carried it and the storage holds the upload to it.
        SniffedImage sniffed = ImageSniffer.sniff(firstBytes)
                .orElseThrow(() -> discardAndRefuse(image, Reason.UNREADABLE_IMAGE, now));
        policy.checkContent(slot, sniffed).ifPresent(reason -> {
            throw discardAndRefuse(image, reason, now);
        });

        // 4. the commit point. Replacing the image comes first: it locks the profile row, so of two commits of one
        // ticket the second sees the ticket already current and has nothing left to do.
        Optional<ImageId> previous = transaction.execute(status -> {
            Optional<ImageId> replaced = profiles.replaceImage(userId, slot, imageId, now);
            if (replaced.filter(imageId::equals).isPresent()) {
                return replaced;
            }
            pending.lockPending(userId, imageId)
                    .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_NOT_FOUND));
            pending.delete(imageId);
            pending.discardPending(userId, slot); // the tickets that lost: their uploads may still arrive
            replaced.ifPresent(old -> pending.discard(new OwnedImage(userId, slot, old), now));
            return replaced;
        });
        if (previous.filter(imageId::equals).isPresent()) {
            return alreadyApplied(userId, slot, imageId);
        }

        // 5. nothing left to do for the replaced key: the transaction recorded it and the sweeper deletes it
        log.info("image committed userId={} slot={} imageId={} replaced={}", userId.value(), slot, text(imageId), previous.isPresent());
        return new Result(Outcome.APPLIED, slot.name(), text(imageId), ImagePath.of(userId, slot, imageId));
    }

    private static Result alreadyApplied(UserId userId, ImageSlot slot, ImageId imageId) {
        return new Result(Outcome.ALREADY_APPLIED, slot.name(), text(imageId), ImagePath.of(userId, slot, imageId));
    }

    /**
     * Retires the rejected file: records in the ledger that its key is due for deletion, which the sweeper carries out,
     * and returns the refusal for the caller to throw. The ledger write happens here, before the throw, so call it only
     * on the way out.
     */
    private ImageUploadRejectedException discardAndRefuse(OwnedImage image, Reason reason, Instant now) {
        pending.discard(image, now);
        return new ImageUploadRejectedException(reason);
    }

    private static String text(ImageId id) {
        return id.value().toString();
    }
}
