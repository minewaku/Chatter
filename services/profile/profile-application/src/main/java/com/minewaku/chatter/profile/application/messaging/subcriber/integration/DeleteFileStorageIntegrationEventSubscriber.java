package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.DeleteFileStorageIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class DeleteFileStorageIntegrationEventSubscriber implements IntegrationEventSubscriber<DeleteFileStorageIntegrationEvent> {

    private final AssetStorage assetStorage;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void handle(DeleteFileStorageIntegrationEvent event) {
        if(processedEventRepository.existsById(event.getEventId())) {
            log.info("Event with ID {} has already been processed. Skipping.", event.getEventId());
            return;
        }

        log.info("Handling DeleteFileStorageIntegrationEvent for fileHash: {}", event.getFileHash());
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.delete(namespace, event.getFileHash());

        processedEventRepository.save(
            event.getEventId(), 
            Instant.now());
    }
}
