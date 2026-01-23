package com.minewaku.chatter.domain.value.id;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class RoleId {
	
	@NonNull
	private final Long value;
	
	public RoleId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("RoleId cannot be smaller than 1");
		}
		
		this.value = value;
	}
}
