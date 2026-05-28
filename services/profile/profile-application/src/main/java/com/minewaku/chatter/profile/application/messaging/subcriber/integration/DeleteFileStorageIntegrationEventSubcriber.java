package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.DeleteFileStorageIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DeleteFileStorageIntegrationEventSubcriber implements IntegrationEventSubscriber<DeleteFileStorageIntegrationEvent> {

    private final AssetStorage assetStorage;

    @Override
    @Transactional
    public void handle(DeleteFileStorageIntegrationEvent event) {
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.delete(namespace, event.getFileHash());
    }
}
