package com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync;

import java.util.List;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public class SyncDomainEventPublisher implements DomainEventPublisher {
	
	private final SyncDomainEventQueue messageQueue;
	
	public SyncDomainEventPublisher(SyncDomainEventQueue messageQueue) {
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
