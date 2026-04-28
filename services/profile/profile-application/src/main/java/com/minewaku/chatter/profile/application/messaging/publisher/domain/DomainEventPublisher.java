package com.minewaku.chatter.profile.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public class DomainEventPublisher {
	
	private final EventQueue eventQueue;
	
	public DomainEventPublisher(EventQueue eventQueue) {
		this.eventQueue = eventQueue;
	}

	public void publish(DomainEvent event) {
		eventQueue.push(event);
	}
	
	public void publish(List<DomainEvent> events) {
		eventQueue.push(events);
	}
}
