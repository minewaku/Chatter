package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.command.profile.CreateUserCommand;
import com.minewaku.chatter.domain.event.core.DomainEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class UserCreatedDomainEvent extends DomainEvent {
	
	@NonNull
	private final CreateUserCommand createUserCommand;
	
	public UserCreatedDomainEvent(@NonNull CreateUserCommand createUserCommand) {
		super("UserCreated");
		this.createUserCommand = createUserCommand;
	}
}
