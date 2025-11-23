package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.event.dto.CreatedUserDto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class UserCreatedDomainEvent extends DomainEvent {
	
	@NonNull
	private final CreatedUserDto createdUserDto;
	
	public UserCreatedDomainEvent(@NonNull CreatedUserDto createdUserDto) {
		super();
		this.createdUserDto = createdUserDto;
	}
}
