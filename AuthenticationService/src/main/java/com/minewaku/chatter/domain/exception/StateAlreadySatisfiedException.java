package com.minewaku.chatter.domain.exception;

import com.minewaku.chatter.domain.exception.core.DomainException;

public class StateAlreadySatisfiedException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "STATE_ALREADY_SATISFIED";

    public StateAlreadySatisfiedException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
