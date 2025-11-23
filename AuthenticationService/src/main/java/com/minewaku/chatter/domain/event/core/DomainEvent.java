package com.minewaku.chatter.domain.event.core;

import java.time.Instant;

import lombok.Getter;

@Getter
public class DomainEvent {
	private final Instant occurredAt;
	
	public DomainEvent() {
		this.occurredAt = Instant.now();
	}
}
