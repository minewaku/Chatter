package com.minewaku.chatter.profile.profile.api.exception;

import java.util.Locale;

/** An upload request or an upload commit was refused. The message never contains a submitted value. */
public class ImageUploadRejectedException extends RuntimeException {

    public enum Reason {
        /** The file's type is not one of the allowed image formats. */
        FORMAT_NOT_ALLOWED,
        /** The file is larger than the slot allows. */
        FILE_TOO_LARGE,
        /** The image is wider or taller than the slot allows. */
        DIMENSIONS_NOT_ALLOWED,
        /** The stored bytes are not an image we can read. */
        UNREADABLE_IMAGE,
        /** No upload ticket of this user has this id (never issued, already used, or discarded). */
        UPLOAD_NOT_FOUND,
        /** The ticket expired; request a new one. */
        UPLOAD_EXPIRED,
        /** The ticket is valid but the file has not arrived at the storage yet. */
        UPLOAD_INCOMPLETE
    }

    private final Reason reason;

    public ImageUploadRejectedException(Reason reason) {
        super("image upload rejected: " + reason.name().toLowerCase(Locale.ROOT));
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
