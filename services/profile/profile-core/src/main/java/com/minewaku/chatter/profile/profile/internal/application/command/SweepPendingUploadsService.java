package com.minewaku.chatter.profile.profile.internal.application.command;

import com.minewaku.chatter.profile.profile.api.command.SweepPendingUploadsUseCase;
import com.minewaku.chatter.profile.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profile.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profile.profile.internal.port.repository.PendingUploadRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deletes the keys the ledger says are no longer needed: abandoned uploads, replaced and removed images, refused
 * files. Each due row is handled in its own transaction under a lock the other workers skip: delete the object (a
 * missing one counts as success), then delete the row. When the delete fails the row is not dropped but postponed, so
 * one stubborn key can neither be lost nor hold up the rows behind it; it is retried until it works.
 */
@Service
public class SweepPendingUploadsService implements SweepPendingUploadsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SweepPendingUploadsService.class);

    private final ImageStorage storage;
    private final PendingUploadRepository pending;
    private final SweepPolicy policy;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public SweepPendingUploadsService(
            ImageStorage storage,
            PendingUploadRepository pending,
            SweepPolicy policy,
            Clock clock,
            TransactionTemplate transaction) {
        this.storage = storage;
        this.pending = pending;
        this.policy = policy;
        this.clock = clock;
        this.transaction = transaction;
    }

    @Override
    public Result handle(Command command) {
        AtomicInteger deleted = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        Instant now = clock.instant();
        for (int handled = 0; handled < policy.batchSize(); handled++) {
            Boolean found = transaction.execute(status -> sweepOne(now, deleted, failed));
            if (!Boolean.TRUE.equals(found)) {
                break;
            }
        }
        return new Result(deleted.get(), failed.get());
    }

    private boolean sweepOne(Instant now, AtomicInteger deleted, AtomicInteger failed) {
        Optional<PendingUpload> due = pending.lockNextDue(now);
        if (due.isEmpty()) {
            return false;
        }
        PendingUpload upload = due.get();
        try {
            storage.delete(upload.image());
            pending.delete(upload.image().imageId());
            deleted.incrementAndGet();
        } catch (RuntimeException e) {
            pending.postpone(upload.image().imageId(), now.plus(policy.retryDelay()));
            failed.incrementAndGet();
            log.error(
                    "image delete failed, will retry userId={} slot={} imageId={}",
                    upload.image().userId().value(),
                    upload.image().slot(),
                    upload.image().imageId().value(),
                    e);
        }
        return true;
    }
}
