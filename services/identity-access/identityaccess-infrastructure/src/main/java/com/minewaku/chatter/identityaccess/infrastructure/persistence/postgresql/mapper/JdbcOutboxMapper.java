package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcOutboxEntity;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
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
        log.info("Mapping IntegrationEventWrapper to JdbcOutboxEntity: id={}, aggregateType={}, aggregateId={}, eventType={}, event={}", 
            wrapper.getId(), wrapper.getAggregateType(), wrapper.getAggregateId(), wrapper.getEventType(), wrapper.getEvent());
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
