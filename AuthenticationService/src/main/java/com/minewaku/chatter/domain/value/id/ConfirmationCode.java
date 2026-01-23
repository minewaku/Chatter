package com.minewaku.chatter.domain.value.id;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class ConfirmationCode {

    @NonNull
	private final String value;
	
	public ConfirmationCode(@NonNull String value) {
		if (value.isBlank()) {
			throw new DomainValidationException("Confirmation code cannot be blank");
        }

		this.value = value;
	}
}
