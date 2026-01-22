package com.minewaku.chatter.application.publisher;

import java.util.List;

import com.minewaku.chatter.domain.event.core.DomainEvent;

public class QueueEventPublisher implements EventPublisher {
	
	private final MessageQueue messageQueue;
	
	public QueueEventPublisher(MessageQueue messageQueue) {
		this.messageQueue = messageQueue;
	}

	@Override
	public void publish(DomainEvent event) {
		messageQueue.push(event);
	}
	
	@Override
	public void publish(List<DomainEvent> events) {
		messageQueue.push(events);
	}
}
