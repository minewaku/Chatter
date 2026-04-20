package com.minewaku.chatter.profile.application.messaging.publisher.integration;

import java.util.List;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;

public interface OutboxStore {
    void push(IntegrationEventWrapper<?> event);
	void push(List<IntegrationEventWrapper<?>> events);
}
