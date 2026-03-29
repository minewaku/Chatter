package com.minewaku.chatter.profile.domain.sharedkernel.exception;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.core.DomainException;

public class BusinessRuleViolationException extends DomainException {
	
    private static final String DEFAULT_ERROR_CODE = "BUSSINESS_RULE_VIOLATION";

	public BusinessRuleViolationException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}