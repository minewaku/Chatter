package com.minewaku.chatter.profilenext.profile.api.exception;

/** The provider told us to back off. The sweep treats it as "stop now, resume next time". */
public class ImageStorageRateLimitedException extends ImageStorageUnavailableException {

    public ImageStorageRateLimitedException(String message) {
        super(message);
    }

    public ImageStorageRateLimitedException(String message, Throwable cause) {
        super(message, cause);
    }
}
