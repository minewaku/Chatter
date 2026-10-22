package com.minewaku.chatter.identityaccess.shared.throttle;

import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.port.AttemptLimiter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Same observable rules as the Redis adapter: every hit counts, an attempt above the maximum is refused, a refused
 * attempt does not extend the window, and {@code clear} forgets the count. Tests end a window with {@link #expire()}.
 */
public class InMemoryAttemptLimiter implements AttemptLimiter {

    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, Integer> totalHits = new HashMap<>();
    private boolean unavailable;

    @Override
    public AttemptOutcome hit(AttemptPolicy policy, String subject) {
        failIfUnavailable();
        String key = key(policy, subject);
        totalHits.merge(key, 1, Integer::sum);
        int count = counts.merge(key, 1, Integer::sum);
        return new AttemptOutcome(count <= policy.maxAttempts(), policy.window());
    }

    @Override
    public void clear(AttemptPolicy policy, String subject) {
        failIfUnavailable();
        counts.remove(key(policy, subject));
    }

    /** Ends every open window, as if the time passed. */
    public void expire() {
        counts.clear();
    }

    /** Ends the open window of one policy and subject only, as if just that window had passed. */
    public void expireOnly(AttemptPolicy policy, String subject) {
        counts.remove(key(policy, subject));
    }

    /** Makes both operations throw, as a store that cannot be reached. */
    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    /** The count in the open window, 0 when none. */
    public int count(AttemptPolicy policy, String subject) {
        return counts.getOrDefault(key(policy, subject), 0);
    }

    /** Every hit ever made, including cleared ones. */
    public int totalHits(AttemptPolicy policy, String subject) {
        return totalHits.getOrDefault(key(policy, subject), 0);
    }

    private void failIfUnavailable() {
        if (unavailable) {
            throw new DependencyUnavailableException(Duration.ofSeconds(10), null);
        }
    }

    private static String key(AttemptPolicy policy, String subject) {
        return policy.name() + "|" + subject;
    }
}
