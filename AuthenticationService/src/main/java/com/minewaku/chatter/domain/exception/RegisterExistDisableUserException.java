package com.minewaku.chatter.domain.exception;

public class RegisterExistDisableUserException extends DomainException {

	private static final String DEFAULT_ERROR_CODE = "REGISTER_EXIST_DISABLE_USER";

    public RegisterExistDisableUserException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
