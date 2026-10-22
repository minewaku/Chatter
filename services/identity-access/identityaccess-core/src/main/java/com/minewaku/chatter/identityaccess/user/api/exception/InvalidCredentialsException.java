package com.minewaku.chatter.identityaccess.user.api.exception;

public final class InvalidCredentialsException extends IllegalArgumentException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
