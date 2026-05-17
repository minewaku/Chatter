package com.minewaku.chatter.message.application.messaging.subcriber.core;

import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEvent;

public interface IntegrationEventSubscriber<T extends IntegrationEvent> {
    void handle(T event);
}
