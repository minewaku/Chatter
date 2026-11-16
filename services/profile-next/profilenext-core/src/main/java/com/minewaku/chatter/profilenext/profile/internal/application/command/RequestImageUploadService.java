package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.UploadTicket;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingUploadRepository;
import com.minewaku.chatter.profilenext.shared.port.UniqueStringIdGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issues an upload ticket. The ledger row is committed <em>before</em> the ticket is signed, so a crash can only leave a
 * row for a key that never received a file, which the sweeper deletes as a no-op; the reverse order could leave a file
 * nothing knows about.
 *
 * <p>A user and slot have at most one pending ticket. Asking again while it is still usable hands out the same image id
 * (retries overwrite one object); asking after it expired retires it and starts a new one.
 */
@Service
public class RequestImageUploadService implements RequestImageUploadUseCase {

    private final ImageStorage storage;
    private final PendingUploadRepository pending;
    private final ImagePolicy policy;
    private final UniqueStringIdGenerator ids;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public RequestImageUploadService(
            ImageStorage storage,
            PendingUploadRepository pending,
            ImagePolicy policy,
            UniqueStringIdGenerator ids,
            Clock clock,
            TransactionTemplate transaction) {
        this.storage = storage;
        this.pending = pending;
        this.policy = policy;
        this.ids = ids;
        this.clock = clock;
        this.transaction = transaction;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        ImageSlot slot = ImageSlot.valueOf(command.slot());
        policy.checkRequest(slot, command.contentType(), command.sizeBytes()).ifPresent(reason -> {
            throw new ImageUploadRejectedException(reason);
        });

        Instant now = clock.instant();
        PendingUpload upload = transaction.execute(status -> reserve(userId, slot, now));

        UploadTicket ticket = storage.issueUpload(
                upload.image(), command.contentType(), command.sizeBytes(), Duration.between(now, upload.expiresAt()));
        return new Result(
                upload.image().imageId().value().toString(),
                ticket.url(),
                ticket.method(),
                ticket.headers(),
                ticket.fields(),
                upload.expiresAt()); // the ledger's expiry is the one commit enforces
    }

    private PendingUpload reserve(UserId userId, ImageSlot slot, Instant now) {
        Optional<PendingUpload> existing = pending.findPending(userId, slot);
        if (existing.isPresent() && existing.get().isReusableAt(now)) {
            return existing.get();
        }
        // an old ticket is retired but keeps its sweep time: its upload may still arrive until it expires
        existing.ifPresent(old -> pending.discard(old.image(), old.deleteAfter()));

        OwnedImage image = new OwnedImage(userId, slot, ImageId.of(ids.nextId()));
        PendingUpload fresh = PendingUpload.issue(image, now, policy.ticketLifetime(), policy.grace());
        if (pending.insertPending(fresh)) {
            return fresh;
        }
        // a concurrent request for the same slot won the unique slot; use its ticket
        return pending.findPending(userId, slot)
                .filter(winner -> winner.isReusableAt(now))
                .orElseThrow(() -> new ImageUploadRejectedException(Reason.UPLOAD_CONFLICT));
    }
}
