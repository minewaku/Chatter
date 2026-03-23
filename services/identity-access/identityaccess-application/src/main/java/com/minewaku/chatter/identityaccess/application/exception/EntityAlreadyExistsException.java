package com.minewaku.chatter.identityaccess.application.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.BusinessException;

public class EntityAlreadyExistsException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "ENTITY_ALREADY_EXISTS";

	public EntityAlreadyExistsException(String message) {
		super(message, DEFAULT_ERROR_CODE);
	}
}
