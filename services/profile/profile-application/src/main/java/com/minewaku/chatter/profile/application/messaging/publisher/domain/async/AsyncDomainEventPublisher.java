package com.minewaku.chatter.profile.application.messaging.publisher.domain.async;

import java.util.List;

import com.minewaku.chatter.profile.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public class AsyncDomainEventPublisher implements DomainEventPublisher {

	private final AsyncDomainEventStore storeEvent;
	
	public AsyncDomainEventPublisher(AsyncDomainEventStore storeEvent) {
		this.storeEvent = storeEvent;
	}

	@Override
	public void publish(DomainEvent event) {
		storeEvent.push(event);
	}
	
	@Override
	public void publish(List<DomainEvent> events) {
		storeEvent.push(events);
	}

}
