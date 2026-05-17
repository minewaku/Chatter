package com.minewaku.chatter.message.domain.model.guild.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.message.domain.sharedkernel.value.Id;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class GuildId implements Id {
	
	@Column("guild_id")
	private final Long value;
	
	@PersistenceCreator
	public GuildId(@NonNull Long value) {
		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("GuildId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}