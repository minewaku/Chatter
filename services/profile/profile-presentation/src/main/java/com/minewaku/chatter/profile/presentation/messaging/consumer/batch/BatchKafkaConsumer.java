package com.minewaku.chatter.profile.presentation.messaging.consumer.batch;

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
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AvatarFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.BannerFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.integration.AvatarFileStorageUploadedIntegrationEventBatchSubscriber;
import com.minewaku.chatter.profile.application.messaging.subcriber.integration.BannerFileStorageUploadedIntegrationEventBatchSubscriber;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class BatchKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AvatarFileStorageUploadedIntegrationEventBatchSubscriber avatarFileStorageUploadedIntegrationEventBatchSubscriber;
    private final BannerFileStorageUploadedIntegrationEventBatchSubscriber bannerFileStorageUploadedIntegrationEventBatchSubscriber;


    @KafkaListener(
        topics = "dev.private.event.profile.file.avatarFileStorageUploaded", 
        groupId = "dev-com.minewaku.profile.file.chatter.event.avatarFileStorageUploaded",
        containerFactory = "batchFactory"
    )
    public void consumeOutboxAvatarFileStorageUploadedIntegrationEventBatch(List<ConsumerRecord<String, String>> records) {
        List<AvatarFileStorageUploadedIntegrationEvent> events = new ArrayList<>();

        try {
            for (ConsumerRecord<String, String> record : records) {
                Header header = record.headers().lastHeader("eventType");
                
                if (header == null || header.value() == null || 
                    !"AvatarFileStorageUploaded".equals(new String(header.value(), StandardCharsets.UTF_8))) {
                    log.error("wrong event type in topic {}, expected avatarFileStorageUploaded but got {}", record.topic(), header == null ? "null" : new String(header.value(), StandardCharsets.UTF_8));
                    continue;
                }

                AvatarFileStorageUploadedIntegrationEvent event = objectMapper.readValue(record.value(), AvatarFileStorageUploadedIntegrationEvent.class);
                events.add(event);
            }

            if (!events.isEmpty()) {
                avatarFileStorageUploadedIntegrationEventBatchSubscriber.handle(events);
            }
        } catch (Exception e) {
            log.error("Error processing batch of events", e);
            throw new RuntimeException(e);
        }
    }


    @KafkaListener(
        topics = "dev.private.event.profile.file.bannerFileStorageUploaded", 
        groupId = "dev-com.minewaku.profile.file.chatter.event.bannerFileStorageUploaded",
        containerFactory = "batchFactory"
    )
    public void consumeOutboxBannerFileStorageUploadedIntegrationEventBatch(List<ConsumerRecord<String, String>> records) {
        List<BannerFileStorageUploadedIntegrationEvent> events = new ArrayList<>();

        try {
            for (ConsumerRecord<String, String> record : records) {
                Header header = record.headers().lastHeader("eventType");
                
                if (header == null || header.value() == null || 
                    !"BannerFileStorageUploaded".equals(new String(header.value(), StandardCharsets.UTF_8))) {
                    log.error("wrong event type in topic {}, expected bannerFileStorageUploaded but got {}", record.topic(), header == null ? "null" : new String(header.value(), StandardCharsets.UTF_8));
                    continue;
                }

                BannerFileStorageUploadedIntegrationEvent event = objectMapper.readValue(record.value(), BannerFileStorageUploadedIntegrationEvent.class);
                events.add(event);
            }

            if (!events.isEmpty()) {
                bannerFileStorageUploadedIntegrationEventBatchSubscriber.handle(events);
            }
        } catch (Exception e) {
            log.error("Error processing batch of events", e);
            throw new RuntimeException(e);
        }
    }


    @KafkaListener(
        topics = "dev.private.event.socket.profile", 
        groupId = "dev-com.minewaku.profile.chatter.event.socket",
        containerFactory = "batchFactory"
    )
    public void consumeGuildOutboxSocketEventsBatch(List<ConsumerRecord<String, String>> records) {
        try {
            for (ConsumerRecord<String, String> record : records) {

                String payload = record.value();
                JsonNode eventNode = objectMapper.readTree(payload);
                String profileId = eventNode.has("profileId") ? eventNode.get("profileId").asText() : null;

                if (profileId == null) {
                    log.warn("Skipping event missing profileId: {}", payload);
                    continue;
                }

                String destination = String.format("/topic/profiles/%s", profileId);
                messagingTemplate.convertAndSend(destination, payload);
            }
        } catch (Exception e) {
            log.error("Error processing batch of events", e);
            throw new RuntimeException(e);
        }
    }
}
