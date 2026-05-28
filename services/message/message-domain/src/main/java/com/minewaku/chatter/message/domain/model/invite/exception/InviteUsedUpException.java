package com.minewaku.chatter.message.domain.model.invite.exception;

import com.minewaku.chatter.message.domain.sharedkernel.exception.core.BusinessException;

public class InviteUsedUpException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "INVITE_USED_UP";

    public InviteUsedUpException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }
}
