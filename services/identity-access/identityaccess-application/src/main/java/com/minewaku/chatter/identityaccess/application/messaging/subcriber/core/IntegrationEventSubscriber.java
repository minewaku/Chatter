package com.minewaku.chatter.identityaccess.application.messaging.subcriber.core;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEvent;

public interface IntegrationEventSubscriber<T extends IntegrationEvent> {
    void handle(T event);
}
