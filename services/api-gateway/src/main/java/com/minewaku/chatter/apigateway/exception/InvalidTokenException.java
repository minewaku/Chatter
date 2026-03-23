package com.minewaku.chatter.apigateway.exception;

public class InvalidTokenException extends RuntimeException {
	public InvalidTokenException(String message) {
		super(message);
	}
	
    public InvalidTokenException(String message, RuntimeException e) {
        super(message, e);
    }
}
