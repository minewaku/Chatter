package com.minewaku.chatter.profile.application.messaging.subcriber.core;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEvent;

public interface IntegrationEventSubscriber<T extends IntegrationEvent> {
    void handle(T event);
}
