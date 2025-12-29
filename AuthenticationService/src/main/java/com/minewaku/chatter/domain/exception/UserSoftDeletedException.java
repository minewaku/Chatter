package com.minewaku.chatter.domain.exception;

public class UserSoftDeletedException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ERROR_CODE = "USER_SOFT_DELETED";

    public UserSoftDeletedException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
