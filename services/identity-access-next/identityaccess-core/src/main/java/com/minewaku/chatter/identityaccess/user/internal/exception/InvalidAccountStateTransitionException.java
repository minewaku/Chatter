package com.minewaku.chatter.identityaccess.user.internal.exception;

import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;

public final class InvalidAccountStateTransitionException extends IllegalStateException {

    public InvalidAccountStateTransitionException(AccountStatus from, String operation) {
        super("Cannot " + operation + " an account in " + from + " state");
    }
}
