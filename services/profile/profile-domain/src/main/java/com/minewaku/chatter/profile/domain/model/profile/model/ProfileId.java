package com.minewaku.chatter.profile.domain.model.profile.model;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Embeddable
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileId {
	
	@NonNull
	private Long value;
	
	public ProfileId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("ProfileId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}
