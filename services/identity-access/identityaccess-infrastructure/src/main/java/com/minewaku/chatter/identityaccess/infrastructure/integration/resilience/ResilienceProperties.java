package com.minewaku.chatter.identityaccess.infrastructure.integration.resilience;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the two circuit breakers. A breaker opens when at least {@code minimumCalls} of the last
 * {@code windowSize} calls were recorded and {@code failureRatePercent} of them failed; it then refuses calls for
 * {@code openDuration} and lets {@code trialCalls} through to decide whether to close again.
 */
@ConfigurationProperties("identity-access.resilience")
public record ResilienceProperties(Breaker mail, Breaker sessionStore) {

    public record Breaker(
            Integer windowSize,
            Integer minimumCalls,
            Integer failureRatePercent,
            Duration openDuration,
            Integer trialCalls) {}

    public ResilienceProperties {
        mail = checked("mail", mail, 10, 5, 50, Duration.ofSeconds(30), 2);
        sessionStore = checked("session-store", sessionStore, 20, 10, 50, Duration.ofSeconds(10), 3);
    }

    private static Breaker checked(
            String name,
            Breaker configured,
            int defaultWindow,
            int defaultMinimum,
            int defaultRate,
            Duration defaultOpen,
            int defaultTrial) {
        Breaker given = configured == null ? new Breaker(null, null, null, null, null) : configured;
        int windowSize = given.windowSize() == null ? defaultWindow : given.windowSize();
        int minimumCalls = given.minimumCalls() == null ? defaultMinimum : given.minimumCalls();
        int rate = given.failureRatePercent() == null ? defaultRate : given.failureRatePercent();
        Duration open = given.openDuration() == null ? defaultOpen : given.openDuration();
        int trial = given.trialCalls() == null ? defaultTrial : given.trialCalls();
        String prefix = "identity-access.resilience." + name;
        if (windowSize < 1) {
            throw new IllegalArgumentException(prefix + ".window-size must be at least 1");
        }
        if (minimumCalls < 1 || minimumCalls > windowSize) {
            throw new IllegalArgumentException(prefix + ".minimum-calls must be between 1 and window-size");
        }
        if (rate < 1 || rate > 100) {
            throw new IllegalArgumentException(prefix + ".failure-rate-percent must be between 1 and 100");
        }
        if (open.isZero() || open.isNegative()) {
            throw new IllegalArgumentException(prefix + ".open-duration must be positive");
        }
        if (trial < 1) {
            throw new IllegalArgumentException(prefix + ".trial-calls must be at least 1");
        }
        return new Breaker(windowSize, minimumCalls, rate, open, trial);
    }
}
