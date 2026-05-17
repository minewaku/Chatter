package com.minewaku.chatter.message.infrastructure.messaging.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.messaging.publisher.domain.EventQueue;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

@Component
public class SpringEventSyncMessageQueue implements EventQueue {

	private final ApplicationEventPublisher eventPublisher;

	public SpringEventSyncMessageQueue(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void dispatch(DomainEvent event) {
		eventPublisher.publishEvent(event);
	}

	@Override
	public void dispatch(List<DomainEvent> events) {
		for (DomainEvent event : events) {
			eventPublisher.publishEvent(event);
		}
	}
}