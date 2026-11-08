package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.RemoveObsoleteImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository.PendingRemoval;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Drains the removal queue. Deliberately not transactional: taking rows is one short transaction in the repository
 * (it leases them), each provider call runs outside any transaction, and each outcome is another short one. A failure
 * here never touches a profile.
 */
@Service
public class RemoveObsoleteImagesService implements RemoveObsoleteImagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemoveObsoleteImagesService.class);

    private final ImageStorage storage;
    private final PendingImageRemovalRepository removals;
    private final RemovalPolicy policy;
    private final Clock clock;

    public RemoveObsoleteImagesService(
            ImageStorage storage, PendingImageRemovalRepository removals, RemovalPolicy policy, Clock clock) {
        this.storage = storage;
        this.removals = removals;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        Instant now = clock.instant();
        List<PendingRemoval> due = removals.takeDue(now, policy.pollBatchSize(), policy.lease());
        int removed = 0;
        int failed = 0;
        int reported = 0;
        for (PendingRemoval row : due) {
            try {
                storage.remove(row.image());
                removals.markRemoved(row.image().imageId());
                removed++;
            } catch (RuntimeException e) {
                failed++;
                if (recordFailure(row, now)) {
                    reported++;
                }
            }
        }
        return new Result(removed, failed, reported);
    }

    /** Returns true when this failure used up the retries and the row was reported. */
    private boolean recordFailure(PendingRemoval row, Instant now) {
        int attempts = row.attempts() + 1;
        if (attempts > policy.maxRetries()) {
            removals.markFailed(row.image().imageId(), attempts, now, now);
            log.error(
                    "image removal gave up imageId={} userId={} attempts={}",
                    row.image().imageId().value(),
                    row.image().userId().value(),
                    attempts);
            return true;
        }
        removals.markFailed(row.image().imageId(), attempts, now.plus(policy.delayBeforeRetry(attempts)), null);
        log.warn(
                "image removal failed, will retry imageId={} userId={} attempts={}",
                row.image().imageId().value(),
                row.image().userId().value(),
                attempts);
        return false;
    }
}
