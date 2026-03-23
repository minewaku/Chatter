package com.minewaku.chatter.identityaccess.domain.aggregate.user.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.DomainException;

public class UserSoftDeletedException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "USER_SOFT_DELETED";

    public UserSoftDeletedException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
