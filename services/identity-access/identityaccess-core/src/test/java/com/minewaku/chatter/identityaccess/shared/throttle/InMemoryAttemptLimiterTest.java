package com.minewaku.chatter.identityaccess.shared.throttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryAttemptLimiterTest {

    private static final AttemptPolicy THREE = new AttemptPolicy("three", 3, Duration.ofMinutes(15));
    private static final AttemptPolicy OTHER = new AttemptPolicy("other", 3, Duration.ofMinutes(15));

    private final InMemoryAttemptLimiter limiter = new InMemoryAttemptLimiter();

    @Test
    void allowsUpToTheMaximumAndRefusesTheNextWithAPositiveWaitNoLongerThanTheWindow() {
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.hit(THREE, "a").allowed());
        }
        AttemptOutcome refused = limiter.hit(THREE, "a");

        assertFalse(refused.allowed());
        assertTrue(refused.retryAfter().compareTo(Duration.ZERO) > 0);
        assertTrue(refused.retryAfter().compareTo(THREE.window()) <= 0);
    }

    @Test
    void clearForgetsTheCount() {
        for (int i = 0; i < 4; i++) {
            limiter.hit(THREE, "a");
        }
        limiter.clear(THREE, "a");

        assertEquals(0, limiter.count(THREE, "a"));
        assertTrue(limiter.hit(THREE, "a").allowed());
    }

    @Test
    void subjectsAndPoliciesAreIndependent() {
        for (int i = 0; i < 4; i++) {
            limiter.hit(THREE, "a");
        }

        assertTrue(limiter.hit(THREE, "b").allowed());
        assertTrue(limiter.hit(OTHER, "a").allowed());
    }

    @Test
    void anExpiredWindowStartsOver() {
        for (int i = 0; i < 4; i++) {
            limiter.hit(THREE, "a");
        }
        limiter.expire();

        assertTrue(limiter.hit(THREE, "a").allowed());
    }

    @Test
    void anUnavailableStoreFailsClosedInsteadOfAllowing() {
        limiter.setUnavailable(true);

        assertThrows(DependencyUnavailableException.class, () -> limiter.hit(THREE, "a"));
        assertThrows(DependencyUnavailableException.class, () -> limiter.clear(THREE, "a"));
    }

    @Test
    void aPolicyRejectsValuesThatCouldNeverLimitAnything() {
        assertThrows(IllegalArgumentException.class, () -> new AttemptPolicy(" ", 1, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new AttemptPolicy("x", 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new AttemptPolicy("x", 1, Duration.ZERO));
    }
}
