package com.minewaku.chatter.identityaccess.shared.exception;

import java.time.Duration;

/** A store that protects the service could not be used, so the request fails closed instead of going unchecked. */
public final class DependencyUnavailableException extends RuntimeException {

    private final Duration retryAfter;

    public DependencyUnavailableException(Duration retryAfter, Throwable cause) {
        super("Dependency unavailable", cause);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
