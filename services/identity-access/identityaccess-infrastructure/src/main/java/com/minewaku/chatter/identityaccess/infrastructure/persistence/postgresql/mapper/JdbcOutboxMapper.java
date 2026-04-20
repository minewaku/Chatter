package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcOutboxEntity;

@Component
public class JdbcOutboxMapper {

    private final ObjectMapper objectMapper;

    public JdbcOutboxMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JdbcOutboxEntity integrationEventWrapperToEntity(IntegrationEventWrapper<? extends IntegrationEvent> wrapper) {
        if (wrapper == null) {
            return null;
        }
        JsonNode payloadNode = objectMapper.valueToTree(wrapper.getEvent());

        return JdbcOutboxEntity.builder()
                .id(UUID.fromString(wrapper.getId()))
                .aggregateType(wrapper.getAggregateType())
                .aggregateId(wrapper.getAggregateId())
                .eventType(wrapper.getEventType())
                .payload(payloadNode)
                .createdAt(wrapper.getOccurredAt()) 
                .build();
    }
}
