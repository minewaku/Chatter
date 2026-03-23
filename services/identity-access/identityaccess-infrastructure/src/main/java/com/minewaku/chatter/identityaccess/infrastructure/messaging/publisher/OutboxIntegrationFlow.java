package com.minewaku.chatter.identityaccess.infrastructure.messaging.publisher;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OutboxIntegrationFlow {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String DLQ_TOPIC = "DEAD_LETTER_QUEUE";

    // best stuff i can find
    // https://javierholguera.com/2024/08/20/naming-kafka-objects-i-topics/
    // https://javierholguera.com/2024/09/12/naming-kafka-objects-ii-producers-and-consumers/
    // https://javierholguera.com/2024/09/25/naming-kafka-objects-iii-kafka-connectors/
    private static final String TOPIC_USER_EVENT = "dev.shared.event.identityaccess.user";


    private static final Map<String, String> TOPIC_ROUTING = Map.of(
            "UserRegistered",  TOPIC_USER_EVENT,
            "UserLocked",      TOPIC_USER_EVENT,
            "UserUnlocked",    TOPIC_USER_EVENT,
            "UserEnabled",     TOPIC_USER_EVENT,
            "UserSoftDeleted", TOPIC_USER_EVENT,
            "UserRestored",    TOPIC_USER_EVENT,
            "UserHardDeleted", TOPIC_USER_EVENT
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

    private void processAndRouteEvent(Message<?> message) {
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
            String actualEventPayload = afterNode.path("payload").asText(); 

            String targetTopic = TOPIC_ROUTING.getOrDefault(eventType, DLQ_TOPIC);

            if (DLQ_TOPIC.equals(targetTopic)) {
                sendToDlq("Unrecognized Event Type: " + eventType, rawPayload);
                return;
            }

            kafkaTemplate.send(targetTopic, aggregateId, actualEventPayload);
            log.info("OUTBOX -> KAFKA: Routed [{}] to [{}] (Key: {})", eventType, targetTopic, aggregateId);

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