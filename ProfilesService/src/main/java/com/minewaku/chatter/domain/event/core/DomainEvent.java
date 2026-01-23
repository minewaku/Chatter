package com.minewaku.chatter.domain.event.core;

import java.time.Instant;

import lombok.Getter;

@Getter
public class DomainEvent {
	private final Instant occurredAt;
	private final String eventType;
	
	public DomainEvent(String eventType) {
		this.occurredAt = Instant.now();
		this.eventType = this.getClass().getSimpleName();
	}
}
