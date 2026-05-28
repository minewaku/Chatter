package com.minewaku.chatter.profile.application.messaging.publisher.domain;

import java.util.List;

import com.minewaku.chatter.profile.application.messaging.publisher.SyncEventDispatcher;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public class DomainEventPublisher {
	
	private final SyncEventDispatcher<DomainEvent> syncDispatcher;
	
	public DomainEventPublisher(SyncEventDispatcher<DomainEvent> syncDispatcher) {
        this.syncDispatcher = syncDispatcher;
    }

	public void publish(DomainEvent event) {
		syncDispatcher.dispatch(event);
	}
	
	public void publish(List<DomainEvent> events) {
		syncDispatcher.dispatch(events);
	}
}
