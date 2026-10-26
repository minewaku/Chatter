package com.minewaku.chatter.identityaccess.authentication.api.exception;

/** The caller's current session is expired, unknown, or not theirs. Does not say which. */
public final class SessionUnusableException extends RuntimeException {

    public SessionUnusableException() {
        super("Session is not usable");
    }
}
