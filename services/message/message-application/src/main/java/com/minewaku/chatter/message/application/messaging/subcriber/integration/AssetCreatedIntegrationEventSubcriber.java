package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetCreatedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;


@Service
public class AssetCreatedIntegrationEventSubcriber implements IntegrationEventSubscriber<AssetCreatedIntegrationEvent> {

    private final AssetStorage assetStorage;

    public AssetCreatedIntegrationEventSubcriber(
            AssetStorage assetStorage) {

        this.assetStorage = assetStorage;
    }

    @Override
    @Transactional
    public void handle(AssetCreatedIntegrationEvent event) {
        Namespace namespace = Namespace.valueOf(event.getNamespace());
        assetStorage.commitUpload(namespace, event.getFileHash(), event.getContext());
    }
}
