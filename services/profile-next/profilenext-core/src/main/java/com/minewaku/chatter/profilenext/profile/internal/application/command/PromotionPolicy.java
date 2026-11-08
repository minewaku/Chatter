package com.minewaku.chatter.profilenext.profile.internal.application.command;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * How a failed promotion is retried: wait {@code delays[k-1]} before retry k (the last delay repeats), until
 * {@code horizon} has passed since the record was created, then give up.
 */
public record PromotionPolicy(Duration horizon, List<Duration> delays, int pollBatchSize, Duration lease) {

    public Duration delayBeforeRetry(int retryNumber) {
        return delays.get(Math.min(retryNumber - 1, delays.size() - 1));
    }

    /** True once the horizon has passed since {@code createdAt}: a failure now is final. */
    public boolean expired(Instant createdAt, Instant now) {
        return !now.isBefore(createdAt.plus(horizon));
    }
}
