package com.minewaku.chatter.profile.domain.sharedkernel.exception;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.core.DomainException;

public class DomainValidationException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "DOMAIN_VALIDATION_ERROR";

    public DomainValidationException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
