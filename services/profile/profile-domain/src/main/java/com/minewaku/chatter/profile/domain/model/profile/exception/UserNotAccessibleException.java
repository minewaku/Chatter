package com.minewaku.chatter.profile.domain.model.profile.exception;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.core.DomainException;

public class UserNotAccessibleException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "USER_NOT_ACCESSIBLE";

	public UserNotAccessibleException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
