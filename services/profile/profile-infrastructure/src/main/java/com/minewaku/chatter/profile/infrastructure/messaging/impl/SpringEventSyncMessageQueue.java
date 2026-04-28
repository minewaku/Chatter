package com.minewaku.chatter.profile.infrastructure.messaging.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.messaging.publisher.domain.EventQueue;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

@Component
public class SpringEventSyncMessageQueue implements EventQueue {

	private final ApplicationEventPublisher eventPublisher;

	public SpringEventSyncMessageQueue(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void push(DomainEvent event) {
		eventPublisher.publishEvent(event);
	}

	@Override
	public void push(List<DomainEvent> events) {
		for (DomainEvent event : events) {
			eventPublisher.publishEvent(event);
		}
	}
}