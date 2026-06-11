package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.messaging.publisher.integration.event.PersistFileStorageIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class PersistFileStorageIntegrationEventSubscriber implements IntegrationEventSubscriber<PersistFileStorageIntegrationEvent> {

    private final AssetStorage assetStorage;

    @Override
    public void handle(PersistFileStorageIntegrationEvent event) {
        log.info("Handling PersistFileStorageIntegrationEvent for fileHash: {}", event.getFileHash());
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.commitUpload(namespace, event.getFileHash());
    }
}
