package com.minewaku.chatter.adapter.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends RuntimeException {

	private String errorCode;
	private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

	public ApiException(String message) {
		super(message);
	}

	public ApiException(String message, HttpStatus httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

	public ApiException(String message, String errorCode, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	public ApiException(String message, String errorCode, HttpStatus httpStatus, Exception cause) {
		super(message, cause);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	public ApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
