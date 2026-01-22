package com.minewaku.chatter.domain.exception;

import com.minewaku.chatter.domain.exception.core.DomainException;

import lombok.Getter;

@Getter
public class InvalidTokenException extends DomainException {

	private static final String DEFAULT_ERROR_CODE = "INVALID_TOKEN";

	public InvalidTokenException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
