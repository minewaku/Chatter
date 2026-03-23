package com.minewaku.chatter.identityaccess.domain.aggregate.user.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.AppException;

public class CompromisedSessionException extends AppException {
    
    private static final String DEFAULT_ERROR_CODE = "INTERNAL_SERVER_ERROR";

	public CompromisedSessionException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
