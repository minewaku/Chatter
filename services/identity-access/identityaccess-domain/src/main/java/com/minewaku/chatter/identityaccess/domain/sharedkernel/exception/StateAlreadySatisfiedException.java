package com.minewaku.chatter.identityaccess.domain.sharedkernel.exception;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core.DomainException;

public class StateAlreadySatisfiedException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "STATE_ALREADY_SATISFIED";

    public StateAlreadySatisfiedException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
