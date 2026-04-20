package com.minewaku.chatter.identityaccess.domain.aggregate.session.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.DomainException;

import lombok.Getter;

@Getter
public class InvalidRefreshTokenException extends DomainException {

	private static final String DEFAULT_ERROR_CODE = "INVALID_TOKEN";

	public InvalidRefreshTokenException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
