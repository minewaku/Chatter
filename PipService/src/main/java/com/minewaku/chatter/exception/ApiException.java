package com.minewaku.chatter.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends RuntimeException {
	
	private String errorCode;
	private int statusCode = HttpStatus.BAD_REQUEST.value();
	private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

	public ApiException(String message) {
		super(message);
	}
	
	public ApiException(String message, String errorCode, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.statusCode = httpStatus.value();
		this.httpStatus = httpStatus;
	}
	
	public ApiException(String message, String errorCode, int statusCode, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.statusCode = statusCode;
		this.httpStatus = httpStatus;
	}


	public ApiException(String message, Throwable cause) {
		super(message, cause);
	}
}

