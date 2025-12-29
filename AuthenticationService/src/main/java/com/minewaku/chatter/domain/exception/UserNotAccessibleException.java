package com.minewaku.chatter.domain.exception;

public class UserNotAccessibleException extends DomainException {

	private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ERROR_CODE = "USER_NOT_ACCESSIBLE";

	public UserNotAccessibleException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
