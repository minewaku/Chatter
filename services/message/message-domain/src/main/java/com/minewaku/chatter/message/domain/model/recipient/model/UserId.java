package com.minewaku.chatter.message.domain.model.recipient.model;



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
public class UserId implements Id{
	
	@Column("user_id")
	private final Long value;
	
	@PersistenceCreator
	public UserId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("UserId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}
