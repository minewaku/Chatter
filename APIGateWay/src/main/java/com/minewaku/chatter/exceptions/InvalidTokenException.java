package com.minewaku.chatter.exceptions;

public class InvalidTokenException extends RuntimeException {
	public InvalidTokenException(String message) {
		super(message);
	}
	
    public InvalidTokenException(String message, RuntimeException e) {
        super(message, e);
    }
}
