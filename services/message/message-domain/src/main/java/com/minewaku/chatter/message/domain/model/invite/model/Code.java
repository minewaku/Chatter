package com.minewaku.chatter.message.domain.model.invite.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.message.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Code implements Id {
	
	@Column("code")
	private final String value;
	
	@PersistenceCreator
	public Code(@NonNull String value) {
		if(value == null || value.trim().isEmpty()) {
			throw new DomainValidationException("Code value cannot be null or empty");
		}
        if(value.length() != 8) {
            throw new DomainValidationException("Code value must be 8 characters long");
        }
		
		this.value = value;
	}
}
