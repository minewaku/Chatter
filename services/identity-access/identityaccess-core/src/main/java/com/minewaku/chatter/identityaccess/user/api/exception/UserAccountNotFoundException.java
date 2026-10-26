package com.minewaku.chatter.identityaccess.user.api.exception;

/** The account a signed-in caller acts for no longer exists. Carries no detail about the caller or the id. */
public final class UserAccountNotFoundException extends RuntimeException {

    public UserAccountNotFoundException() {
        super("User account not found");
    }
}
