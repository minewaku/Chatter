package com.minewaku.chatter.adapter.messaging.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OutboxIntegrationFlow {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String HEADER_EVENT_TYPE = "event_type";
    private static final String HEADER_KAFKA_TOPIC = "kafka_topic";
    private static final String DLQ_TOPIC = "DEAD_LETTER_QUEUE";

    private static final String TOPIC_USER_ENTITY = "dev.shared.entity.authentication.user.id";
    private static final String TOPIC_USER_ACCESS = "dev.shared.command.authentication.accessibility.id";

    @Bean
    public IntegrationFlow outboxFlow() {
        return IntegrationFlow.from("outboxChannel")
                .log(LoggingHandler.Level.INFO, "com.minewaku.chatter", 
                        m -> "OUTBOX RECEIVED: " + m.getPayload())
                .enrichHeaders(h -> h.headerFunction(HEADER_EVENT_TYPE, this::extractEventType))
                .enrichHeaders(h -> h.headerFunction(HEADER_KAFKA_TOPIC, this::resolveTopic))
                .log(LoggingHandler.Level.INFO, "com.minewaku.chatter", 
                        m -> "ROUTING [" + m.getHeaders().get(HEADER_EVENT_TYPE) + "] TO: " + m.getHeaders().get(HEADER_KAFKA_TOPIC))
                .handle(this::sendToKafka)
                .get();
    }

    private String extractEventType(Message<?> message) {
        try {
            JsonNode root = objectMapper.readTree((String) message.getPayload());
            
            JsonNode eventTypeNode = root.path("payload").path("after").path("event_type");
            
            if (eventTypeNode.isMissingNode() || eventTypeNode.isNull()) {
                log.warn("Missing 'event_type' in payload. Raw JSON: {}", root);
                return null;
            }
            
            return eventTypeNode.asText();
        } catch (Exception e) {
            log.error("JSON Parse Error", e);
            return null;
        }
    }

    private String resolveTopic(Message<?> message) {
        String eventType = (String) message.getHeaders().get(HEADER_EVENT_TYPE);

        if (!StringUtils.hasText(eventType)) {
            return DLQ_TOPIC;
        }

        if (eventType.contains("UserCreated")) {
            return TOPIC_USER_ENTITY;
        }

        if (eventType.contains("UserLocked") || 
            eventType.contains("UserUnlocked") ||
            eventType.contains("UserSoftDeleted") ||
            eventType.contains("UserRestored") || 
            eventType.contains("UserHardDeleted")) {
            return TOPIC_USER_ACCESS;
        }

        return DLQ_TOPIC;
    }

    private Object sendToKafka(Object payload, MessageHeaders headers) {
        String topic = (String) headers.get(HEADER_KAFKA_TOPIC);
        String eventType = (String) headers.get(HEADER_EVENT_TYPE);

        if (topic == null || DLQ_TOPIC.equals(topic)) {
            log.error("DROPPED MESSAGE (DLQ). EventType: {}, Payload: {}", eventType, payload);
            return null;
        }

        try {
            kafkaTemplate.send(topic, (String) payload);
        } catch (Exception e) {
            log.error("KAFKA SEND ERROR for Topic: {}", topic, e);
            throw new RuntimeException("Failed to send to Kafka", e);
        }
        return null;
    }
}