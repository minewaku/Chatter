package com.minewaku.chatter.profile.domain.model.profile.model;


import org.springframework.data.annotation.PersistenceCreator;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.profile.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class ProfileId implements Id {
	
	private final Long value;
	
	@PersistenceCreator
	public ProfileId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("ProfileId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}
