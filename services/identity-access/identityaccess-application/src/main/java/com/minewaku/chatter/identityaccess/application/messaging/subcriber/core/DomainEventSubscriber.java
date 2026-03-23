package com.minewaku.chatter.identityaccess.application.messaging.subcriber.core;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

public interface DomainEventSubscriber<T extends DomainEvent> {
    void handle(T event);
}
