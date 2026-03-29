package com.minewaku.chatter.profile.domain.sharedkernel.exception;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.core.DomainException;

public class ResourceNotFoundException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
