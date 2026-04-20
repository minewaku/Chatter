package com.minewaku.chatter.identityaccess.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public interface EventQueue {
	void push(DomainEvent event);
	void push(List<DomainEvent> events);
}
