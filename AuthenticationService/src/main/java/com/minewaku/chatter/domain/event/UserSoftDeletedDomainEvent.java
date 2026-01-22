package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class UserSoftDeletedDomainEvent extends DomainEvent {
	
	private static final String EVENT_TYPE = "UserSoftDeleted";

	@NonNull
	private final UserId userId;
	
	public UserSoftDeletedDomainEvent(@NonNull UserId userId) {
		super(EVENT_TYPE);
		this.userId = userId;
	}
}
