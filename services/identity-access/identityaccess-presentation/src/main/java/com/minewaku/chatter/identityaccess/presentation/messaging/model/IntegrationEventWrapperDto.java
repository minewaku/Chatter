package com.minewaku.chatter.identityaccess.presentation.messaging.model;

import com.fasterxml.jackson.databind.JsonNode;

public record IntegrationEventWrapperDto (
    String id,
    String aggregateId,
    String aggregateType,
    String eventType,
    JsonNode event,
    String occurredAt
) {}
