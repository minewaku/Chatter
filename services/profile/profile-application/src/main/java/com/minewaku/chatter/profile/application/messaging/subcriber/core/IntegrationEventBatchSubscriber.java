package com.minewaku.chatter.profile.application.messaging.subcriber.core;

import java.util.List;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEvent;

public interface IntegrationEventBatchSubscriber<T extends IntegrationEvent> {
    void handle(List<T> events);
}
