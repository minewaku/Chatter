package com.minewaku.chatter.profilenext.profile.api.exception;

/** A provider notification was refused before anything was written. */
public class UploadConfirmationRejectedException extends RuntimeException {

    public enum Reason {
        INVALID_SIGNATURE,
        USER_MISMATCH
    }

    private final Reason reason;

    public UploadConfirmationRejectedException(Reason reason) {
        super("upload confirmation rejected: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
