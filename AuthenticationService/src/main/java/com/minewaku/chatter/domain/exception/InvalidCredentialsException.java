package com.minewaku.chatter.domain.exception;

public class InvalidCredentialsException extends DomainException {

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_ERROR_CODE = "INVALID_CREDENTIALS";

	public InvalidCredentialsException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
