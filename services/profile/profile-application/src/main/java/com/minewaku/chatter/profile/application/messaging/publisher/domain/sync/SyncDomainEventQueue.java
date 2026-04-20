package com.minewaku.chatter.profile.application.messaging.publisher.domain.sync;

import java.util.List;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public interface SyncDomainEventQueue {
	void push(DomainEvent event);
	void push(List<DomainEvent> events);
}
