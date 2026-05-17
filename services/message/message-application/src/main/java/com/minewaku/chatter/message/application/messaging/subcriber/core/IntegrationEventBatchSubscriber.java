package com.minewaku.chatter.message.application.messaging.subcriber.core;

import java.util.List;

import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEvent;

public interface IntegrationEventBatchSubscriber<T extends IntegrationEvent> {
    void handle(List<T> events);
}