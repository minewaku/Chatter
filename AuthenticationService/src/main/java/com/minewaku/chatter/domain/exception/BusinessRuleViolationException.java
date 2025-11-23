package com.minewaku.chatter.domain.exception;

public class BusinessRuleViolationException extends RuntimeException {
	
	private static final long serialVersionUID = -5899240007810338539L;

	public BusinessRuleViolationException(String message) {
        super(message);
    }
}