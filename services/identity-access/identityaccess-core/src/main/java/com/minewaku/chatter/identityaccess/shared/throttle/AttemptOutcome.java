package com.minewaku.chatter.identityaccess.shared.throttle;

import java.time.Duration;

/** The verdict for one counted attempt. {@code retryAfter} is the remaining window and is meaningful when refused. */
public record AttemptOutcome(boolean allowed, Duration retryAfter) {

    public AttemptOutcome {
        if (retryAfter == null || retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative");
        }
    }
}
