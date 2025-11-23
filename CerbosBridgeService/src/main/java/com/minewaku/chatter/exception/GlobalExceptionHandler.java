package com.minewaku.chatter.exception;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import com.minewaku.chatter.web.response.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = {ApiException.class})
    public ResponseEntity<Object> handleException(ApiException e) {
        ErrorResponse errorDto = new ErrorResponse(
                                            e.getMessage(), 
                                            ZonedDateTime.now(ZoneId.of("Z"))
        );

        log.error(e.getMessage(), e);
        return new ResponseEntity<>(errorDto, e.getHttpStatus());
    }

    @ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class})
    public ResponseEntity<ErrorResponse> handleHttpStatusException(HttpStatusCodeException e) {
        ErrorResponse errorDto = new ErrorResponse(
                e.getMessage(),
                ZonedDateTime.now(ZoneOffset.UTC)   // "Z" → UTC
        );

        log.error("External service error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);

        return new ResponseEntity<>(errorDto, e.getStatusCode());
    }

    @ExceptionHandler(value = {IllegalArgumentException.class})
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse errorDto = new ErrorResponse(
                                        e.getMessage(), 
                                        ZonedDateTime.now(ZoneId.of("Z"))
        );

        log.error(e.getMessage(), e);
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {AccessDeniedException.class})
	public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException e) {
		ErrorResponse errorDto = new ErrorResponse(
										e.getMessage(), 
										ZonedDateTime.now(ZoneId.of("Z")));

		log.error(e.getMessage(), e);
		return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
	}

    
    @ExceptionHandler(value = {RuntimeException.class})
    public ResponseEntity<Object> handleOtherException(Exception e) {
        ErrorResponse errorDto = new ErrorResponse(
                                        e.getMessage(), 
                                        ZonedDateTime.now(ZoneId.of("Z"))
        );

        log.error(e.getMessage(), e);
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
