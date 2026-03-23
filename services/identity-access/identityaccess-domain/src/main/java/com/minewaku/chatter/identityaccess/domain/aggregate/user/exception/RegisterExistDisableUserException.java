package com.minewaku.chatter.identityaccess.domain.aggregate.user.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.DomainException;

public class RegisterExistDisableUserException extends DomainException {

	private static final String DEFAULT_ERROR_CODE = "REGISTER_EXIST_DISABLE_USER";

    public RegisterExistDisableUserException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
