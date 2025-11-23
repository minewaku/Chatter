package com.minewaku.chatter.adapter.event;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.domain.event.core.DomainEvent;

@Component
public class SpringEventMessageQueue implements MessageQueue {
	
	private final ApplicationEventPublisher eventPublisher;

	public SpringEventMessageQueue(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}
	
	@Override
	public void push(DomainEvent event) {
		eventPublisher.publishEvent(event);
	}

	@Override
	public void push(List<DomainEvent> events) {
		for(DomainEvent event : events) {
			eventPublisher.publishEvent(event);
		}
	}

	@Override
	public DomainEvent pop() {
		// TODO Auto-generated method stub
		return null;
	}

}
