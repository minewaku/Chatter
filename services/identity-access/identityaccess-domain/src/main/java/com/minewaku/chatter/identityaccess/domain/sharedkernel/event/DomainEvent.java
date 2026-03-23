package com.minewaku.chatter.identityaccess.domain.sharedkernel.event;

import java.time.Instant;

import lombok.Getter;

@Getter
public class DomainEvent {
	private final Instant occurredAt;
	
	public DomainEvent() {
		this.occurredAt = Instant.now();
	}
}
