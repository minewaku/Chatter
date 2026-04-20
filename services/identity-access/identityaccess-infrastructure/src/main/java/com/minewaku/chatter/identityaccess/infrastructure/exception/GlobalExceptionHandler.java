package com.minewaku.chatter.identityaccess.infrastructure.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpServerErrorException;

import com.minewaku.chatter.identityaccess.application.exception.DataInconsistencyException;
import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.exception.CompromisedSessionException;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.exception.InvalidRefreshTokenException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.RegisterExistDisableUserException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.UserAlreadyExistException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.UserNotAccessibleException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler {
    

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(BusinessRuleViolationException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(DomainValidationException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }



    @ExceptionHandler(CompromisedSessionException.class)
    public ResponseEntity<ErrorResponse> handleValidation(CompromisedSessionException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleValidation(InvalidRefreshTokenException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }



    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleValidation(InvalidCredentialsException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleValidation(UserAlreadyExistException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RegisterExistDisableUserException.class)
    public ResponseEntity<ErrorResponse> handleValidation(RegisterExistDisableUserException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UserNotAccessibleException.class)
    public ResponseEntity<ErrorResponse> handleValidation(UserNotAccessibleException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }


    
    @ExceptionHandler(DataInconsistencyException.class)
    public ResponseEntity<ErrorResponse> handleValidation(DataInconsistencyException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    


    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleValidation(EntityNotFoundException ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex);
        return build(HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleValidation(AccessDeniedException ex) {
        log.error("Access denied error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse("ACCESS_DENIED", "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Validation error: {}", ex);
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_SERVER_ERROR", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        log.error("Validation error: {}", message);
        return ResponseEntity.status(status).body(body);
    }


    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/ConcurrencyFailureException.html
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleFallback(ConcurrencyFailureException ex) {
            ErrorResponse errorResponse = new ErrorResponse("CONFLICT", "Conflict detected: The resource version is outdated.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/HttpServerErrorException.html
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleFallback(HttpServerErrorException ex) {
        ErrorResponse errorResponse = new ErrorResponse("SERVICE_UNAVAILABLE", "Service temporarily unavailable, please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }


    @Getter
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private Instant timestamp = Instant.now();

        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }
}
