package com.minewaku.chatter.identityaccess.infrastructure.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.AppException;

public class MalformRefreshTokenException extends AppException {
    public MalformRefreshTokenException(String message) {
        super(message);
    }

    public MalformRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
