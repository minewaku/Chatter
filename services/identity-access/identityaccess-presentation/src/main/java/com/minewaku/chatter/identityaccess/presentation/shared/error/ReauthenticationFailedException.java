package com.minewaku.chatter.identityaccess.presentation.shared.error;

/**
 * Raised by controllers of authenticated re-authentication endpoints when core refuses a wrong password, an
 * inaccessible account, or an unusable current session. Mapped to {@code 403} so clients do not mistake it for an
 * expired access credential.
 */
public final class ReauthenticationFailedException extends RuntimeException {

    public ReauthenticationFailedException(Throwable cause) {
        super("Reauthentication failed", cause);
    }
}
