package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.PersistFileStorageIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class PersistFileStorageIntegrationEventSubscriber implements IntegrationEventSubscriber<PersistFileStorageIntegrationEvent> {

    private final AssetStorage assetStorage;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    public void handle(PersistFileStorageIntegrationEvent event) {
        if(processedEventRepository.existsById(event.getEventId())) {
            log.info("Event with ID {} has already been processed. Skipping.", event.getEventId());
            return;
        }

        log.info("Handling PersistFileStorageIntegrationEvent for fileHash: {}", event.getFileHash());
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.commitUpload(namespace, event.getFileHash());

        processedEventRepository.save(
            event.getEventId(), 
            Instant.now());
    }
}
