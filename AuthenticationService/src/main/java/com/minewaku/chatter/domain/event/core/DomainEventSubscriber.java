package com.minewaku.chatter.domain.event.core;

public interface DomainEventSubscriber<T extends DomainEvent> {
    void handle(T event);
}

