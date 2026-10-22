package com.minewaku.chatter.identityaccess.shared.exception;

import java.time.Duration;

/** A limit refused the attempt. Carries only the wait; never the subject that was counted. */
public final class TooManyAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super("Too many attempts");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
