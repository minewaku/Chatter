package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import java.time.Instant;

import lombok.Getter;

@Getter
public class IntegrationEventWrapper<T extends IntegrationEvent> {

    private final String id;
    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final T event;
    private final Instant occurredAt;

    public IntegrationEventWrapper(String id, String aggregateId, T event) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = event.getAggregateType();
        this.eventType = event.getEventType();
        this.event = event;
        this.occurredAt = Instant.now();
    }
}
