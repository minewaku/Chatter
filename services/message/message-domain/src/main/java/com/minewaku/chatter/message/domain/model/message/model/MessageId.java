package com.minewaku.chatter.message.domain.model.message.model;

import org.springframework.data.annotation.PersistenceCreator;

import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.message.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class MessageId implements Id {
	
	private final Long value;
	
	@PersistenceCreator
	public MessageId(@NonNull Long value) {
		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("MessageId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}