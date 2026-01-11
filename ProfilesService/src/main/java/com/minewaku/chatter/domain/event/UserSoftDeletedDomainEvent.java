package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class UserSoftDeletedDomainEvent extends DomainEvent {
	
	@NonNull
	private final UserId userId;
	
	public UserSoftDeletedDomainEvent(@NonNull UserId userId) {
		super("UserSoftDeleted");
		this.userId = userId;
	}
}
