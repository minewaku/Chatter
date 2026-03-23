package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class UserId {
	
	@NonNull
	private final Long value;
	
	public UserId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("UserId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}
