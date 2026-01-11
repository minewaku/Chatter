package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.event.dto.CreateUserDto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class UserCreatedDomainEvent extends DomainEvent {
	
	@NonNull
	private final CreateUserDto createdUserDto;
	
	public UserCreatedDomainEvent(@NonNull CreateUserDto createdUserDto) {
		super("");
		this.createdUserDto = createdUserDto;
	}
}
