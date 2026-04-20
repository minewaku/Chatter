package com.minewaku.chatter.profile.application.messaging.subcriber.core;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public interface DomainEventSubscriber<T extends DomainEvent> {
    void handle(T event);
}
