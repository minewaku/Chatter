package com.minewaku.chatter.profile.infrastructure.exception;

public class FileStorageException extends RuntimeException {

	public FileStorageException(String message, Exception cause) {
		super(message, cause);
	}
}
