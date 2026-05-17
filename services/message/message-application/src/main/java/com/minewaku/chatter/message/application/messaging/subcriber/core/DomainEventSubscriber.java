package com.minewaku.chatter.message.application.messaging.subcriber.core;

import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

public interface DomainEventSubscriber<T extends DomainEvent> {
    void handle(T event);
}
