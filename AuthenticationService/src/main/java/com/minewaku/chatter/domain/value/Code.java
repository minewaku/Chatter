package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Code {

	private static final String CODE_REGEX = "^[A-Z0-9_-]{2,12}$";
	private static final Pattern CODE_PATTERN = Pattern.compile(CODE_REGEX);

	@Getter
	@NonNull
	private final String value;

	@JsonCreator
	public Code(
			@JsonProperty("value") @NonNull String value) {

		if (value.isBlank()) {
			throw new DomainValidationException("Code cannot be blank");
		}
		if (!CODE_PATTERN.matcher(value).matches()) {
			throw new DomainValidationException(
					"Invalid code format. "
							+ "Code must be 2 to 12 characters long and contain only uppercase letters (A-Z), "
							+ "digits (0-9), hyphens (-), or underscores (_).");
		}

		this.value = value;
	}
}
