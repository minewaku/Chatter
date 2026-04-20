package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import java.time.Instant;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class IntegrationEventWrapper<T extends IntegrationEvent> {

    private String id;
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    private T event;
    private Instant occurredAt;

    public IntegrationEventWrapper(String id, String aggregateId, T event) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = event.getAggregateType();
        this.eventType = event.getEventType();
        this.event = event;
        this.occurredAt = Instant.now();
    }
}
