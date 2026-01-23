package com.minewaku.chatter.domain.exception.core;

public class DomainException extends BusinessException {

    public DomainException(String message, String errorCode) {
        super(message, errorCode);
    }
}
