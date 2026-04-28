package com.minewaku.chatter.profile.infrastructure.messaging.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;

@Component
public class SpringEventOutboxStore implements OutboxStore {

	private final ApplicationEventPublisher eventPublisher;

	public SpringEventOutboxStore(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}
	
	@Override
	public void push(IntegrationEventWrapper<?> event) {
		eventPublisher.publishEvent(event);
	}

	@Override
	public void push(List<IntegrationEventWrapper<?>> events) {
		for(IntegrationEventWrapper<?> event : events) {
			eventPublisher.publishEvent(event);
		}
	}
}