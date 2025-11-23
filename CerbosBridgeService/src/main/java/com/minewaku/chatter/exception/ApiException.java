package com.minewaku.chatter.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends RuntimeException {
	
	private int statusCode = HttpStatus.BAD_REQUEST.value();
	private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

	public ApiException(String message) {
		super(message);
	}
	
	public ApiException(String message, HttpStatus httpStatus) {
		super(message);
		this.statusCode = httpStatus.value();
	}

	public ApiException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
		this.httpStatus = HttpStatus.valueOf(statusCode);
	}
	
	public ApiException(String message, int statusCode, HttpStatus httpStatus) {
		super(message);
		this.statusCode = statusCode;
		this.httpStatus = httpStatus;
	}


	public ApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
