package com.minewaku.chatter.identityaccess.presentation.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.ConfirmationTokenCreatedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.integration.ConfirmationTokenCreatedIntegrationEventSubcriber;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaConsumer {

    private final ConfirmationTokenCreatedIntegrationEventSubcriber confirmationTokenCreatedIntegrationEventSubcriber;
    private final ObjectMapper objectMapper;

    public KafkaConsumer(
            ConfirmationTokenCreatedIntegrationEventSubcriber confirmationTokenCreatedIntegrationEventSubcriber,
            ObjectMapper objectMapper) {
                
        this.confirmationTokenCreatedIntegrationEventSubcriber = confirmationTokenCreatedIntegrationEventSubcriber;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "dev.internal.event.identityaccess.outbox", groupId = "dev-com.minewaku.identityaccess.chatter.outbox")
    public void consumeUserEvents(
            @Payload String payload, 
            @Header(value = "eventType", required = false) String eventType) {
            
        try {
            if (eventType == null) {
                return;
            }

            switch (eventType) {
                case "ConfirmationTokenCreated": {
                    ConfirmationTokenCreatedIntegrationEvent eventData = 
                            objectMapper.readValue(payload, ConfirmationTokenCreatedIntegrationEvent.class);
                    confirmationTokenCreatedIntegrationEventSubcriber.handle(eventData);
                    break;
                }
                default:
                    log.debug("default skip: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Kafka error: {}", payload, e);
        }
    }

}
