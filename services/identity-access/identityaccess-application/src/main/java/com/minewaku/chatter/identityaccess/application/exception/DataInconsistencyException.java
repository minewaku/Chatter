package com.minewaku.chatter.identityaccess.application.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.AppException;

public class DataInconsistencyException extends AppException {
	
	private static final String DEFAULT_ERROR_CODE = "INTERNAL_SERVER_ERROR";

	public DataInconsistencyException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
