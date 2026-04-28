package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.FileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

@Service
public class FileStorageUploadedIntegrationEventSubcriber implements IntegrationEventSubscriber<FileStorageUploadedIntegrationEvent> {

    private final AssetStorage assetStorage;

    public FileStorageUploadedIntegrationEventSubcriber(
            AssetStorage assetStorage) {

        this.assetStorage = assetStorage;
    }

    @Override
    @Transactional
    public void handle(FileStorageUploadedIntegrationEvent event) {
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.commitUpload(namespace, event.getFileHash(), event.getContext());
    }
}
