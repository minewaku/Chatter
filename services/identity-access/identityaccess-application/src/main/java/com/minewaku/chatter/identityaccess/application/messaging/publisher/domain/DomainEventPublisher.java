package com.minewaku.chatter.identityaccess.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
	void publish(List<DomainEvent> events);
}
