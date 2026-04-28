package com.minewaku.chatter.profile.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public interface EventQueue {
	void push(DomainEvent event);
	void push(List<DomainEvent> events);
}
