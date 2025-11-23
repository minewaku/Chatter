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
public class Code {
	
	private static final String CODE_REGEX = "^[A-Z0-9_-]{6,12}$";
	private static final Pattern CODE_PATTERN = Pattern.compile(CODE_REGEX);
	
	@Getter
	@NonNull
	private final String value;
	
	@JsonCreator
	public Code(
			@JsonProperty("value")@NonNull String value) {

        if(value.isBlank()) {
        	throw new IllegalArgumentException("Code cannot be blank");
        }
        if (!CODE_PATTERN.matcher(value).matches()) {
        	throw new IllegalArgumentException(
        			"Invalid code format. "
        			+ "Code must be 6 to 12 characters long and contain only uppercase letters (A-Z), "
        			+ "digits (0-9), hyphens (-), or underscores (_).");
        }
		
		this.value = value;
	}
}
