package com.minewaku.chatter.application.publisher;

import com.minewaku.chatter.domain.event.core.DomainEvent;

public interface EventPublisher {
	void publish(DomainEvent event);
}
