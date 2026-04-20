package com.minewaku.chatter.profile.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
	void publish(List<DomainEvent> events);
}
