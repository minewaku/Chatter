package com.minewaku.chatter.profile.application.exception;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.core.BusinessException;

public class EntityNotFoundException extends BusinessException {

	private static final String DEFAULT_ERROR_CODE = "ENTITY_NOT_FOUND";

	public EntityNotFoundException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
