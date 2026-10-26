package com.minewaku.chatter.identityaccess.authentication.api.exception;

/**
 * Generic denial thrown by sign-in and renewal only; never reveals whether an account, password, or session exists.
 */
public final class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
