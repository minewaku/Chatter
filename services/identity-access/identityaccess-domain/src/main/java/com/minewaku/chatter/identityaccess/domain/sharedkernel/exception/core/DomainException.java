package com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.core;

public class DomainException extends BusinessException {

    public DomainException(String message, String errorCode) {
        super(message, errorCode);
    }
}
