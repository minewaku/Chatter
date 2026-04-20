package com.minewaku.chatter.profile.application.messaging.publisher.domain.async;

import java.util.List;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public interface AsyncDomainEventStore {
	void push(DomainEvent event);
	void push(List<DomainEvent> events);
}
