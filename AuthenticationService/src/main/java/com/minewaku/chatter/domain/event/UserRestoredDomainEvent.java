package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class UserRestoredDomainEvent extends DomainEvent {
    @NonNull
	private final UserId userId;
	
	public UserRestoredDomainEvent(@NonNull UserId userId) {
		super();
		this.userId = userId;
	}
}
