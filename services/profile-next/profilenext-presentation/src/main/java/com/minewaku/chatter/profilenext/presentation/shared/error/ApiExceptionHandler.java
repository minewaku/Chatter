package com.minewaku.chatter.profilenext.presentation.shared.error;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.api.exception.InvalidProfileValueException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every failure to the single {@link ApiError} body. Messages are fixed per {@link ErrorCode}: submitted values
 * and core exception messages are never echoed. Only the code, the exception class and a correlation id are logged.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthenticated() {
        return body(ErrorCode.UNAUTHENTICATED);
    }

    @ExceptionHandler(InvalidIdentifierException.class)
    ResponseEntity<ApiError> invalidIdentifier(InvalidIdentifierException exception) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiError.of(
                        ErrorCode.VALIDATION_FAILED,
                        List.of(new ApiError.FieldError(
                                exception.field(), "The user id must be a positive whole number."))));
    }

    /** The message states the rule and the real limits and never the submitted value. */
    @ExceptionHandler(InvalidProfileValueException.class)
    ResponseEntity<ApiError> invalidProfileValue(InvalidProfileValueException exception) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiError.of(
                        ErrorCode.VALIDATION_FAILED,
                        List.of(new ApiError.FieldError(exception.field(), exception.getMessage()))));
    }

    /**
     * A refused upload or commit. A file the rules reject is a 422 naming the rule in a fixed sentence; a ticket that is
     * unknown, expired, unfinished or contended has its own status, so a client knows whether to retry or start over.
     */
    @ExceptionHandler(ImageUploadRejectedException.class)
    ResponseEntity<ApiError> imageRejected(ImageUploadRejectedException exception) {
        return switch (exception.reason()) {
            case UPLOAD_NOT_FOUND -> body(ErrorCode.NOT_FOUND);
            case UPLOAD_EXPIRED -> body(ErrorCode.UPLOAD_EXPIRED);
            case UPLOAD_INCOMPLETE -> body(ErrorCode.UPLOAD_INCOMPLETE);
            case UPLOAD_CONFLICT -> body(ErrorCode.CONFLICT);
            default -> ResponseEntity.status(ErrorCode.IMAGE_REJECTED.status())
                    .body(ApiError.of(
                            ErrorCode.IMAGE_REJECTED,
                            List.of(new ApiError.FieldError("image", imageRuleMessage(exception.reason())))));
        };
    }

    private static String imageRuleMessage(ImageUploadRejectedException.Reason reason) {
        return switch (reason) {
            case FORMAT_NOT_ALLOWED -> "The image must be a JPEG, PNG or WebP file.";
            case FILE_TOO_LARGE -> "The file is larger than this image allows.";
            case DIMENSIONS_NOT_ALLOWED -> "The image is wider or taller than this image allows.";
            case UNREADABLE_IMAGE -> "The file could not be read as an image.";
            case CONTENT_TYPE_MISMATCH -> "The file is not the type it was declared as.";
            default -> "The image was rejected.";
        };
    }

    @ExceptionHandler(ImageStorageUnavailableException.class)
    ResponseEntity<ApiError> storageUnavailable() {
        return body(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(ConstraintViolationException exception) {
        List<ApiError.FieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldError(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiError.of(ErrorCode.VALIDATION_FAILED, errors));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) throws Exception {
        if (exception instanceof AccessDeniedException) {
            throw exception; // authorization failures belong to the security chain
        }
        String correlationId = UUID.randomUUID().toString();
        log.error(
                "code={} exception={} correlationId={}",
                ErrorCode.INTERNAL_ERROR.code(),
                exception.getClass().getName(),
                correlationId,
                exception);
        return body(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiError.of(ErrorCode.VALIDATION_FAILED, errors));
    }

    /**
     * Framework exceptions (unreadable body, wrong media type or method, unknown path, missing parameter) are
     * rewritten into the standard shape; the framework's own message is dropped.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = codeFor(statusCode);
        return ResponseEntity.status(code.status()).headers(headers).body(ApiError.of(code));
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            default -> ErrorCode.MALFORMED_REQUEST;
        };
    }

    private static ResponseEntity<ApiError> body(ErrorCode code) {
        return ResponseEntity.status(code.status()).body(ApiError.of(code));
    }
}
