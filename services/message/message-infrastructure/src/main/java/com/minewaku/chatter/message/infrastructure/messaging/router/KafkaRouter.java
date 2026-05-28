package com.minewaku.chatter.message.infrastructure.messaging.router;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaRouter {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Các hằng số Topic
    private static final String DLQ_TOPIC = "dev.internal.event.message.dlq";
    private static final String ATTACHMENT_FILE_UPLOADED_WEBHOOK_NOTIFICATION = "dev.private.event.message.attachment.attachmentFileStorageUploaded";
    private static final String GUILD_ICON_FILE_UPLOADED_WEBHOOK_NOTIFICATION = "dev.private.event.message.guild.guildIconStorageUploaded";

    // Cấu hình Routing
    private static final Map<String, List<String>> TOPIC_ROUTING = Map.of(
            "AttachmentFileStorageUploaded", List.of(ATTACHMENT_FILE_UPLOADED_WEBHOOK_NOTIFICATION),
            "GuildIconFileStorageUploaded", List.of(GUILD_ICON_FILE_UPLOADED_WEBHOOK_NOTIFICATION)
    );

    /**
     * Hàm chính để định tuyến và gửi event vào Kafka
     */
    public void routeAndSend(IntegrationEventWrapper<?> event) {
        String eventType = event.getEventType();
        List<String> targetTopics = TOPIC_ROUTING.get(eventType);

        if (targetTopics == null || targetTopics.isEmpty()) {
            log.warn("No Kafka topic routing found for event type: {}", eventType);
            return;
        }

        try {
            // Chuyển đổi payload thành JSON
            String payloadJson = objectMapper.writeValueAsString(event.getEvent());

            for (String targetTopic : targetTopics) {
                Message<String> kafkaMessage = MessageBuilder
                        .withPayload(payloadJson)
                        .setHeader(KafkaHeaders.TOPIC, targetTopic)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                        .setHeader("eventType", eventType)
                        .setHeader("occurredAt", event.getOccurredAt().toString())
                        .build();

                try {{
                    kafkaTemplate.send(kafkaMessage).get();
                }} catch (Exception e) {
                    log.error("Failed to send event to Kafka. Event ID: {}, Topic: {}", event.getId(), targetTopic, e);
                    // recheck
                    Message<String> dlqMessage = MessageBuilder
                            .withPayload(payloadJson)
                            .setHeader(KafkaHeaders.TOPIC, DLQ_TOPIC)
                            .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                            .setHeader("eventType", eventType)
                            .setHeader("occurredAt", event.getOccurredAt().toString())
                            .setHeader("errorReason", e.getMessage())
                            .build();
                    kafkaTemplate.send(dlqMessage);
                    throw new RuntimeException("Failed to send event to Kafka, sent to DLQ instead. Event ID: " + event.getId(), e);
                }

                log.info("ROUTED TO KAFKA: Dispatched [{}] to [{}] (Key: {})",
                        eventType, targetTopic, event.getAggregateId());
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload for Kafka dispatch. Event ID: {}", event.getId(), e);

        }
    }
}



