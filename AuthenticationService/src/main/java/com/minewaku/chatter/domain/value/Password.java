package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Password {
	private static final String PASSWORD_REGEX = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";
	private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);
	
	@Getter
	@NonNull
	private final String value;
	
	@JsonCreator
	public Password(
			@JsonProperty("value") @NonNull String value) {

        if(value.isBlank()) {
        	throw new IllegalArgumentException("Password cannot be blank");
        }
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
        	throw new IllegalArgumentException(
        		    "Invalid password format. Password must be at least 8 characters long, " +
        		    "contain at least one uppercase letter, one lowercase letter, one digit, " +
        		    "and one special character (#?!@$%^&*-)."
        		);
        }
		
		this.value = value;
	}
}
