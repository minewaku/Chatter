package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class SessionId {
	
	@NonNull
	private final String value;
	
	public SessionId(@NonNull String value) {

		if(value == null || value.isEmpty()) {
			throw new DomainValidationException("SessionId value cannot be null or empty");
		}
		
		this.value = value;
	}
}
