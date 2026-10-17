package com.minewaku.chatter.identityaccess.presentation.authentication;

/** {@code status} is {@code verified} or {@code already_verified}. */
public record VerificationStatusResponse(String status) {

    public static VerificationStatusResponse verified() {
        return new VerificationStatusResponse("verified");
    }

    public static VerificationStatusResponse alreadyVerified() {
        return new VerificationStatusResponse("already_verified");
    }
}
