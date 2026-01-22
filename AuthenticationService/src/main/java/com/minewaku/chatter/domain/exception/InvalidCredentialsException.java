package com.minewaku.chatter.domain.exception;

import com.minewaku.chatter.domain.exception.core.DomainException;

public class InvalidCredentialsException extends DomainException {

	private static final String DEFAULT_ERROR_CODE = "INVALID_CREDENTIALS";

	public InvalidCredentialsException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
