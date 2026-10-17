package com.minewaku.chatter.identityaccess.presentation.shared.error;

import org.springframework.http.HttpStatus;

/** Stable machine-readable error codes. Each message is fixed and never contains submitted values. */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "The request contains invalid values."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request could not be read."),
    INVALID_OR_EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "The token is invalid or has expired."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed."),
    REAUTHENTICATION_FAILED(HttpStatus.FORBIDDEN, "The request could not be confirmed."),
    ACCOUNT_INACCESSIBLE(HttpStatus.FORBIDDEN, "The account cannot perform this action."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "The session was not found."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The resource was not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The method is not allowed for this resource."),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with these details already exists."),
    VERIFICATION_PENDING(HttpStatus.CONFLICT, "The account is awaiting email verification."),
    PASSWORD_UNCHANGED(HttpStatus.CONFLICT, "The password was not changed."),
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
