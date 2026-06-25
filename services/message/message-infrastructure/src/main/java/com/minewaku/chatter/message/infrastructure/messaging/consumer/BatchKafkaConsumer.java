package com.minewaku.chatter.message.infrastructure.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AttachmentFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.subcriber.integration.AttachmentFileStorageUploadedIntegrationEventBatchSubscriber;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class BatchKafkaConsumer {
    
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AttachmentFileStorageUploadedIntegrationEventBatchSubscriber attachmentFileStorageUploadedIntegrationEventBatchSubscriber;

    @KafkaListener(
        topics = "dev.private.event.message.file.attachmentFileStorageUploaded", 
        groupId = "dev-com.minewaku.message.file.chatter.event.attachmentFileStorageUploaded",
        containerFactory = "batchFactory"
    )
    public void consumeOutboxEventsBatch(List<ConsumerRecord<String, String>> records) {
        List<AttachmentFileStorageUploadedIntegrationEvent> events = new ArrayList<>();

        try {
            for (ConsumerRecord<String, String> record : records) {
                Header header = record.headers().lastHeader("eventType");
                
                if (header == null || header.value() == null || 
                    !"AttachmentFileStorageUploaded".equals(new String(header.value(), StandardCharsets.UTF_8))) {
                    log.error("wrong event type in topic {}, expected FileStorageUploaded but got {}", record.topic(), header == null ? "null" : new String(header.value(), StandardCharsets.UTF_8));
                    continue;
                }

                AttachmentFileStorageUploadedIntegrationEvent event = objectMapper.readValue(record.value(), AttachmentFileStorageUploadedIntegrationEvent.class);
                events.add(event);
            }

            if (!events.isEmpty()) {
                attachmentFileStorageUploadedIntegrationEventBatchSubscriber.handle(events);
            }
        } catch (Exception e) {
            log.error("Error processing batch of events", e);
            throw new RuntimeException(e);
        }
    }


    @KafkaListener(
        topics = "dev.private.event.socket.message.guild", 
        groupId = "dev-com.minewaku.message.guild.chatter.event.socket",
        containerFactory = "batchFactory"
    )
    public void consumeGuildOutboxSocketEventsBatch(List<ConsumerRecord<String, String>> records) {
        try {
            for (ConsumerRecord<String, String> record : records) {

                String payload = record.value();
                JsonNode eventNode = objectMapper.readTree(payload);
                String guildId = eventNode.has("guildId") ? eventNode.get("guildId").asText() : null;

                if (guildId == null) {
                    log.warn("Skipping event missing guildId: {}", payload);
                    continue;
                }

                String destination = String.format("/topic/guilds/%s", guildId);
                messagingTemplate.convertAndSend(destination, payload);
            }
        } catch (Exception e) {
            log.error("Error processing batch of events", e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
        topics = "dev.private.event.socket.message.channel", 
        groupId = "dev-com.minewaku.message.channel.chatter.event.socket",
        containerFactory = "batchFactory"
    )
    public void consumeMessageOutboxSocketEventsBatch(List<ConsumerRecord<String, String>> records) {

        try {
            for (ConsumerRecord<String, String> record : records) {

                String payload = record.value();
                JsonNode eventNode = objectMapper.readTree(payload);
                String channelId = eventNode.has("channelId") ? eventNode.get("channelId").asText() : null;

                if (channelId == null) {
                    log.warn("Skipping event missing channelId: {}", payload);
                    continue;
                }

                String destination = String.format("/topic/channels/%s", channelId);
                messagingTemplate.convertAndSend(destination, payload);
            }
        } catch (Exception e) {
            log.error("Error processing batch of events", e);
            throw new RuntimeException(e);
        }
    }
}
