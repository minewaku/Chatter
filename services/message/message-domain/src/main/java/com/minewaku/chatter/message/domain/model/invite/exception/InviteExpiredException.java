package com.minewaku.chatter.message.domain.model.invite.exception;

import com.minewaku.chatter.message.domain.sharedkernel.exception.core.BusinessException;

public class InviteExpiredException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "INVITE_EXPIRED";

    public InviteExpiredException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
    
}
