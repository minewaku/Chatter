package com.minewaku.chatter.identityaccess.infrastructure.messaging.router;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OutboxKafkaRouter {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // best stuff i can find
    // https://javierholguera.com/2024/08/20/naming-kafka-objects-i-topics/
    // https://javierholguera.com/2024/09/12/naming-kafka-objects-ii-producers-and-consumers/
    // https://javierholguera.com/2024/09/25/naming-kafka-objects-iii-kafka-connectors/
    private static final String DLQ_TOPIC = "DEAD_LETTER_QUEUE";
    private static final String TOPIC_USER_EVENT = "dev.shared.event.identityaccess.user";
    private static final String INTERNAL_TOPIC = "dev.internal.event.identityaccess.outbox";

    private static final Map<String, List<String>> TOPIC_ROUTING = Map.of(
            "UserRegistered",  List.of(TOPIC_USER_EVENT),
            "UserSoftDeleted", List.of(TOPIC_USER_EVENT, INTERNAL_TOPIC),
            "EnablementUpdated", List.of(TOPIC_USER_EVENT),
            "ConfirmationTokenCreated", List.of(INTERNAL_TOPIC)
    );

    @Bean
    public IntegrationFlow outboxFlow() {
        return IntegrationFlow.from("outboxChannel")
                .handle(Message.class, (message, headers) -> {
                    processAndRouteEvent(message);
                    return null;
                })
                .get();
    }

    private void processAndRouteEvent(org.springframework.messaging.Message<?> message) {
        String rawPayload = (String) message.getPayload();

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode afterNode = root.path("payload").path("after");

            if (afterNode.isMissingNode() || afterNode.isNull()) {
                sendToDlq("Invalid CDC Format", rawPayload);
                return;
            }

            String eventType = afterNode.path("event_type").asText();
            String aggregateId = afterNode.path("aggregate_id").asText();
            String payload = afterNode.path("payload").asText();

            List<String> targetTopics = TOPIC_ROUTING.get(eventType);

            if (targetTopics == null || targetTopics.isEmpty()) {
                sendToDlq("Unrecognized Event Type: " + eventType, rawPayload);
                return;
            }

            for (String targetTopic : targetTopics) {
                org.springframework.messaging.Message<String> kafkaMessage = MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.TOPIC, targetTopic)
                        .setHeader(KafkaHeaders.KEY, aggregateId)
                        .setHeader("eventType", eventType)
                        .build();

                kafkaTemplate.send(kafkaMessage);
                
                log.info("OUTBOX -> KAFKA: Routed [{}] to [{}] (Key: {})", eventType, targetTopic, aggregateId);
            }

        } catch (Exception e) {
            log.error("Failed to process Outbox message. Sending to DLQ.", e);
            sendToDlq("Processing Exception: " + e.getMessage(), rawPayload);
        }
    }

    private void sendToDlq(String reason, String rawPayload) {
        log.warn("DROPPED MESSAGE (DLQ). Reason: {}. Payload: {}", reason, rawPayload);
        kafkaTemplate.send(DLQ_TOPIC, rawPayload);
    }
}