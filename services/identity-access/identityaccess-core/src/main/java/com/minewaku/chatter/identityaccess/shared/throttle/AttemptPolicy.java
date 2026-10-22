package com.minewaku.chatter.identityaccess.shared.throttle;

import java.time.Duration;

/**
 * How many attempts one subject may make within a window. The window is also the cooling-off period: once the limit
 * is reached, attempts stay refused until the window that started with the first attempt ends.
 */
public record AttemptPolicy(String name, int maxAttempts, Duration window) {

    public AttemptPolicy {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Policy name must not be blank");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Policy " + name + " must allow at least one attempt");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Policy " + name + " must have a positive window");
        }
    }
}
