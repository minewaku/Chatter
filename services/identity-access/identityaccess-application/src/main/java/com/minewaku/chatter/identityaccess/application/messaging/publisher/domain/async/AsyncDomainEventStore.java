package com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.async;

import java.util.List;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public interface AsyncDomainEventStore {
	void push(DomainEvent event);
	void push(List<DomainEvent> events);
}
