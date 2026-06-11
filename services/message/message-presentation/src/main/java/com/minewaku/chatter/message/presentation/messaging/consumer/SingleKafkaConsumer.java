package com.minewaku.chatter.message.presentation.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.DeleteFileStorageIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.GuildIconFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.PersistFileStorageIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.subcriber.integration.AssetAttachedIntegrationEventSubscriber;
import com.minewaku.chatter.message.application.messaging.subcriber.integration.AssetDetachedIntegrationEventSubscriber;
import com.minewaku.chatter.message.application.messaging.subcriber.integration.DeleteFileStorageIntegrationEventSubscriber;
import com.minewaku.chatter.message.application.messaging.subcriber.integration.GuildIconFileStorageUploadedIntegrationEventSubscriber;
import com.minewaku.chatter.message.application.messaging.subcriber.integration.PersistFileStorageIntegrationEventSubscriber;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class SingleKafkaConsumer {

    private final ObjectMapper objectMapper;

    private final GuildIconFileStorageUploadedIntegrationEventSubscriber guildIconFileStorageUploadedIntegrationEventSubscriber;

    private final AssetAttachedIntegrationEventSubscriber assetAttachedIntegrationEventSubscriber;
    private final AssetDetachedIntegrationEventSubscriber assetDetachedIntegrationEventSubscriber;

    private final PersistFileStorageIntegrationEventSubscriber persistFileStorageIntegrationEventSubscriber;
    private final DeleteFileStorageIntegrationEventSubscriber deleteFileStorageIntegrationEventSubscriber;


    @KafkaListener(
        topics = "dev.private.event.message.file.guildIconFileStorageUploaded", 
        groupId = "dev-com.minewaku.message.file.chatter.event.guildIconFileStorageUploaded",
        containerFactory = "singleFactory"
    )
    public void consumeAssetEvents(
            @Payload String payload, 
            @Header(value = "eventType", required = false) String eventType) {
            
        try {
            if (eventType == null) {
                return;
            }

            switch (eventType) {
                case "GuildIconFileStorageUploaded": {
                    GuildIconFileStorageUploadedIntegrationEvent eventData = 
                            objectMapper.readValue(payload, GuildIconFileStorageUploadedIntegrationEvent.class);
                    guildIconFileStorageUploadedIntegrationEventSubscriber.handle(eventData);
                    break;
                }
                default:
                    log.debug("default skip: {}", eventType);
            }

        } catch (JsonProcessingException e) {
            log.error("Unprocessed message, Payload: {}", payload, e);
        }catch (Exception e) {
            log.error("Kafka error: {}", payload, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
        topics = "dev.internal.event.message.outbox", 
        groupId = "dev-com.minewaku.message.chatter.outbox",
        containerFactory = "singleFactory"
    )
    public void consumeOutboxEvents(
            @Payload String payload, 
            @Header(value = "eventType", required = false) String eventType) {
                
        try {
            if (eventType == null) return;

            switch (eventType) {
                case "AssetAttached":
                    AssetAttachedIntegrationEvent assetAttached = objectMapper.readValue(payload, AssetAttachedIntegrationEvent.class);
                    assetAttachedIntegrationEventSubscriber.handle(assetAttached);
                    break;
                case "AssetDetached":
                    AssetDetachedIntegrationEvent assetDetached = objectMapper.readValue(payload, AssetDetachedIntegrationEvent.class);
                    assetDetachedIntegrationEventSubscriber.handle(assetDetached);
                    break;
                case "PersistFileStorage":
                    PersistFileStorageIntegrationEvent persistFile = objectMapper.readValue(payload, PersistFileStorageIntegrationEvent.class);
                    persistFileStorageIntegrationEventSubscriber.handle(persistFile);
                    break;
                case "DeleteFileStorage":
                    DeleteFileStorageIntegrationEvent deleteFile = objectMapper.readValue(payload, DeleteFileStorageIntegrationEvent.class);
                    deleteFileStorageIntegrationEventSubscriber.handle(deleteFile);
                    break;
                default:
                    log.debug("default skip: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Kafka error: {}", eventType, e);
            throw new RuntimeException(e);
        }
    }

}
