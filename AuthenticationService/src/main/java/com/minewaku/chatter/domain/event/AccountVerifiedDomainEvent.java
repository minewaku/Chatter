package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class AccountVerifiedDomainEvent extends DomainEvent {
	
	@NonNull
	private final UserId userId;
	
    
    public AccountVerifiedDomainEvent(@NonNull UserId userId) {
    	super();
    	this.userId = userId;
    }
}
