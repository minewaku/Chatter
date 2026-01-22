package com.minewaku.chatter.domain.exception.core;

import java.util.Objects;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ERROR_CODE = "INTERNAL_SERVER_ERROR";
    private final String errorCode;

    public AppException(
                String message, 
                String errorCode,
                boolean enableSuppression,
                boolean writableStackTrace) {

        super(message, null, enableSuppression, writableStackTrace);
        this.errorCode = Objects.requireNonNullElse(errorCode, DEFAULT_ERROR_CODE);
    }

    public AppException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(String message, Throwable cause) {
        super(message, cause); 
        this.errorCode = DEFAULT_ERROR_CODE;
    }
    
    public AppException(String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}
