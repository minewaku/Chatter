package com.minewaku.chatter.domain.exception.core;

public class BusinessException extends AppException {

    public BusinessException(String message, String errorCode) {
        super(message, errorCode, false, false);
    }

    @Override 
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
