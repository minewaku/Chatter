package com.minewaku.chatter.profilenext.profile.api.exception;

/** The image storage provider could not be reached or refused a call. Never a verdict on the file itself. */
public class ImageStorageUnavailableException extends RuntimeException {

    public ImageStorageUnavailableException(String message) {
        super(message);
    }

    public ImageStorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
