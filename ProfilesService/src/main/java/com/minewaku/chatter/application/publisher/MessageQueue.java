package com.minewaku.chatter.application.publisher;

import java.util.List;

import com.minewaku.chatter.domain.event.core.DomainEvent;

public interface MessageQueue {
	void push(DomainEvent event);
	void push(List<DomainEvent> events);
}
