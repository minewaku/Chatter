package com.minewaku.chatter.identityaccess.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public class DomainEventPublisher{
	
	private final EventQueue messageQueue;
	
	public DomainEventPublisher(EventQueue messageQueue) {
		this.messageQueue = messageQueue;
	}

	public void publish(DomainEvent event) {
		messageQueue.push(event);
	}
	
	public void publish(List<DomainEvent> events) {
		messageQueue.push(events);
	}
}
