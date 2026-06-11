package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.messaging.publisher.integration.event.DeleteFileStorageIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class DeleteFileStorageIntegrationEventSubscriber implements IntegrationEventSubscriber<DeleteFileStorageIntegrationEvent> {

    private final AssetStorage assetStorage;

    @Override
    @Transactional
    public void handle(DeleteFileStorageIntegrationEvent event) {
         log.info("Handling DeleteFileStorageIntegrationEvent for fileHash: {}", event.getFileHash());
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.delete(namespace, event.getFileHash());
    }
}
