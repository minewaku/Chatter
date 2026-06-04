package com.minewaku.chatter.message.presentation.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
    private final AttachmentFileStorageUploadedIntegrationEventBatchSubscriber attachmentFileStorageUploadedIntegrationEventBatchSubscriber;

    //RECHECK: IMPLEMENT SPECIFIC CONSUMER FOR FILESTORAGEUPLOADEDEVENT INSTEAD OF USING GENERIC CONSUMER, TO AVOID UNNECESSARY DESERIALIZATION AND ERROR HANDLING FOR OTHER EVENT TYPES
    @KafkaListener(
        topics = "dev.private.event.message.file.AttachmentFileStorageUploaded", 
        groupId = "dev-com.minewaku.message.file.chatter.event.AttachmentFileStorageUploaded",
        containerFactory = "batchFactory"
    )
    public void consumeOutboxEventsBatch(List<ConsumerRecord<String, String>> records) {
        List<AttachmentFileStorageUploadedIntegrationEvent> events = new ArrayList<>();

        try {
            for (ConsumerRecord<String, String> record : records) {
                Header header = record.headers().lastHeader("eventType");
                
                if (header == null || header.value() == null || 
                    !"FileStorageUploaded".equals(new String(header.value(), StandardCharsets.UTF_8))) {
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
}
