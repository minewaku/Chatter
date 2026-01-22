package com.minewaku.chatter.application.exception;

import com.minewaku.chatter.domain.exception.core.AppException;

public class DataInconsistencyException extends AppException {
	
	private static final String DEFAULT_ERROR_CODE = "INTERNAL_SERVER_ERROR";

	public DataInconsistencyException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
