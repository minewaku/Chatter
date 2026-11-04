package com.minewaku.chatter.profilenext.presentation.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes: the subset of identity-access's vocabulary this service uses, with the same
 * names, statuses and messages (spec 012 FR-015). Each message is fixed and never contains submitted values.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "The request contains invalid values."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request could not be read."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The resource was not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The method is not allowed for this resource."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The content type is not supported."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    /** The lowercase wire identifier, for example {@code validation_failed}. */
    public String code() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
