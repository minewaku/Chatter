package com.minewaku.chatter.application.publisher;

import java.util.List;

import com.minewaku.chatter.domain.event.core.DomainEvent;

public interface EventPublisher {
	void publish(DomainEvent event);
	void publish(List<DomainEvent> events);
}
