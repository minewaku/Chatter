package com.minewaku.chatter.message.infrastructure.persistence.postgresql.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.entity.OutboxJdbcEntity;

@Component
public class OutboxJdbcMapper {

    private final ObjectMapper objectMapper;

    public OutboxJdbcMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxJdbcEntity integrationEventWrapperToEntity(IntegrationEventWrapper<? extends IntegrationEvent> wrapper) {
        if (wrapper == null) {
            return null;
        }
        
        //writeValueAsString turn class into json string which really different than toString() format;
        String payloadNode;
        try {
            payloadNode = objectMapper.writeValueAsString(wrapper.getEvent());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }

        return OutboxJdbcEntity.builder()
                .id(UUID.fromString(wrapper.getId()))
                .aggregateType(wrapper.getAggregateType())
                .aggregateId(wrapper.getAggregateId())
                .eventType(wrapper.getEventType())
                .payload(payloadNode)
                .createdAt(wrapper.getOccurredAt()) 
                .build();
    }
}
