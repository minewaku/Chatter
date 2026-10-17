package com.minewaku.chatter.identityaccess.authentication.api.exception;

/** Generic denial; never reveals whether an account, password, or session exists. */
public final class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
