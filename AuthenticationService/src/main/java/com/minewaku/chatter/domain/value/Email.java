package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Email {
	
	private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
	
	@NonNull
	private final String value;
	
	public Email(@NonNull String value) {
		
        if(value.isBlank()) {
        	throw new DomainValidationException("email cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new DomainValidationException("invalid email format");
        }
		
		this.value = value;
	}
}
