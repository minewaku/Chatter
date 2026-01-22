package com.minewaku.chatter.application.publisher;

import java.util.List;

import com.minewaku.chatter.domain.event.core.DomainEvent;

public class StoreEventPublisher implements EventPublisher {

	private final StoreEvent storeEvent;
	
	public StoreEventPublisher(StoreEvent storeEvent) {
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
