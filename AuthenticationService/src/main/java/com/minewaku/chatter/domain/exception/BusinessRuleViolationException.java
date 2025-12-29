package com.minewaku.chatter.domain.exception;

public class BusinessRuleViolationException extends DomainException {
	
	private static final long serialVersionUID = -5899240007810338539L;
    private static final String DEFAULT_ERROR_CODE = "BUSSINESS_RULE_VIOLATION";

	public BusinessRuleViolationException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}