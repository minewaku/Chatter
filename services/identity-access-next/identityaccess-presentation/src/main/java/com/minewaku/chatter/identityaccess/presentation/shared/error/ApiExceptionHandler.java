package com.minewaku.chatter.identityaccess.presentation.shared.error;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
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
 * Maps every failure to the single {@link ApiError} body. Messages are fixed per {@link ErrorCode}: submitted
 * values and core exception messages are never echoed. Only the code, exception class name, and a correlation id are
 * logged.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiError> authenticationFailed() {
        return body(ErrorCode.AUTHENTICATION_FAILED);
    }

    @ExceptionHandler(ReauthenticationFailedException.class)
    ResponseEntity<ApiError> reauthenticationFailed() {
        return body(ErrorCode.REAUTHENTICATION_FAILED);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials() {
        return body(ErrorCode.REAUTHENTICATION_FAILED);
    }

    @ExceptionHandler({InvalidAccountStateTransitionException.class, AccountInaccessibleException.class})
    ResponseEntity<ApiError> accountInaccessible() {
        return body(ErrorCode.ACCOUNT_INACCESSIBLE);
    }

    @ExceptionHandler(DuplicateUserAccountException.class)
    ResponseEntity<ApiError> duplicateAccount() {
        return body(ErrorCode.ACCOUNT_ALREADY_EXISTS);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthenticated() {
        return body(ErrorCode.UNAUTHENTICATED);
    }

    /** Value-object rules in core (email, password, username, birthday) throw this; its message is never shown. */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidValue() {
        return body(ErrorCode.VALIDATION_FAILED);
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
                correlationId);
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
