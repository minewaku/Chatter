package com.minewaku.chatter.message.infrastructure.messaging.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.messaging.publisher.domain.EventQueue;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@AllArgsConstructor
public class SpringEventSyncMessageQueue implements EventQueue {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void dispatch(DomainEvent event) {
		log.info("Pushing single domain event: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }

    public void dispatch(List<? extends DomainEvent> events) {
		log.info("Pushing batch of domain events: {}", events.size());
        for (DomainEvent event : events) {
            eventPublisher.publishEvent(event);
        }
    }
}