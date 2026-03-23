package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration;

import java.util.List;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;

public class IntegrationEventPublisher {
	
	private final OutboxStore outboxStore;
	
	public IntegrationEventPublisher(OutboxStore outboxStore) {
		this.outboxStore = outboxStore;
	}

	public void publish(IntegrationEventWrapper<?> event) {
		outboxStore.push(event);
	}
	
	public void publish(List<IntegrationEventWrapper<?>> events) {
		outboxStore.push(events);
	}
}
