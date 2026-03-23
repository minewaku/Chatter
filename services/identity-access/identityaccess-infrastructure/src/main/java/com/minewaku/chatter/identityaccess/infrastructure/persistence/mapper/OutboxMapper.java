package com.minewaku.chatter.identityaccess.infrastructure.persistence.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.infrastructure.entity.JpaOutboxEntity;

@Component
public class OutboxMapper {

    private final ObjectMapper objectMapper;

    public OutboxMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JpaOutboxEntity integrationEventWrapperToEntity(IntegrationEventWrapper<? extends IntegrationEvent> wrapper) {
        if (wrapper == null) {
            return null;
        }
        JsonNode payloadNode = objectMapper.valueToTree(wrapper.getEvent());

        return JpaOutboxEntity.builder()
                .aggregateType(wrapper.getAggregateType())
                .aggregateId(wrapper.getAggregateId())
                .eventType(wrapper.getEventType())
                .payload(payloadNode)
                .createdAt(Instant.parse(wrapper.getOccurredAt())) 
                .build();
    }
}
