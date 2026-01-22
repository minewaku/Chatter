package com.minewaku.chatter.domain.value.id;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class OpaqueToken {
    
    @NonNull
	private final String value;
	
	public OpaqueToken(@NonNull String value) {
		if (value.isBlank()) {
            throw new DomainValidationException("Opaque token cannot be blank");
        }

		this.value = value;
	}
}
