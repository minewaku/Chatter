package com.minewaku.chatter.profilenext.profile.internal.application.command;

import java.time.Duration;
import java.util.List;

/**
 * How failed removals are retried: up to {@code maxRetries} retries after the first attempt, waiting
 * {@code delays[k-1]} before retry k (the last delay repeats).
 */
public record RemovalPolicy(int maxRetries, List<Duration> delays, int pollBatchSize, Duration lease) {

    public Duration delayBeforeRetry(int retryNumber) {
        return delays.get(Math.min(retryNumber - 1, delays.size() - 1));
    }
}
