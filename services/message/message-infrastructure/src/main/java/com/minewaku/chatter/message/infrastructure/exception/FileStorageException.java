package com.minewaku.chatter.message.infrastructure.exception;

public class FileStorageException extends RuntimeException {

	public FileStorageException(String message, Exception cause) {
		super(message, cause);
	}
}
