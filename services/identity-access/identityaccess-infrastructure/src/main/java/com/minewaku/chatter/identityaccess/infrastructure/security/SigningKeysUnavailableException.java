package com.minewaku.chatter.identityaccess.infrastructure.security;

/**
 * The signing key pair could not be obtained, so the service must not start. The message names the secret location,
 * the field and the reason; it never holds key text or a fragment of it.
 */
public class SigningKeysUnavailableException extends IllegalStateException {

    public enum Reason {
        VAULT_DISABLED,
        STORE_UNREACHABLE,
        CREDENTIAL_REJECTED,
        SECRET_NOT_FOUND,
        FIELD_MISSING,
        KEY_INVALID,
        KEY_PAIR_MISMATCH
    }

    private final Reason reason;

    public SigningKeysUnavailableException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public SigningKeysUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
