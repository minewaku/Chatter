package com.minewaku.chatter.profilenext.profile.api.exception;

import java.util.Locale;

/**
 * An upload request or an upload commit was refused. The message never contains a submitted value; {@link #ruleName()}
 * is the stable name clients and logs use.
 */
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
        /** The stored bytes are a different format than the one the upload declared. */
        CONTENT_TYPE_MISMATCH,
        /** No upload ticket of this user has this id (never issued, already used, or discarded). */
        UPLOAD_NOT_FOUND,
        /** The ticket expired; request a new one. */
        UPLOAD_EXPIRED,
        /** The ticket is valid but the file has not arrived at the storage yet. */
        UPLOAD_INCOMPLETE,
        /** Two requests for the same slot collided; repeat the call. */
        UPLOAD_CONFLICT
    }

    private final Reason reason;

    public ImageUploadRejectedException(Reason reason) {
        super("image upload rejected: " + reason.name().toLowerCase(Locale.ROOT));
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    /** Lower-case snake form of the reason, for example {@code file_too_large}. */
    public String ruleName() {
        return reason.name().toLowerCase(Locale.ROOT);
    }
}
