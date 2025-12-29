package com.minewaku.chatter.application.exception;

import lombok.Getter;

public class EntityNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	@Getter
	private final String errorCode = "ENTITY_NOT_FOUND";

	public EntityNotFoundException(String message) {
		super(message);
	}
}
