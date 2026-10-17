package com.minewaku.chatter.identityaccess.user.api.exception;

public final class DuplicateUserAccountException extends RuntimeException {

    public DuplicateUserAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
