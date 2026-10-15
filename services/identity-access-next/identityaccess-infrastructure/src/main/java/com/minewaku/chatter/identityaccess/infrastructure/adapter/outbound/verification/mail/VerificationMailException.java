package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

/** Delivery failure. The message never contains the verification token or the verification link. */
class VerificationMailException extends RuntimeException {

    VerificationMailException(String message, Throwable cause) {
        super(message, cause);
    }
}
