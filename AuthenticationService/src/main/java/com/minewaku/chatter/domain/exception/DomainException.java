package com.minewaku.chatter.domain.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public DomainException(String message, String errorCode) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    @Override 
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
