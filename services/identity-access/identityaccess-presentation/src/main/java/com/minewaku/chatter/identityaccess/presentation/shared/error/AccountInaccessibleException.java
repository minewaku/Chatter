package com.minewaku.chatter.identityaccess.presentation.shared.error;

/** Raised by controllers when core refuses an authenticated caller because the account is no longer accessible. */
public final class AccountInaccessibleException extends RuntimeException {

    public AccountInaccessibleException(Throwable cause) {
        super("Account inaccessible", cause);
    }
}
