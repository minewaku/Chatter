package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;

public class AssetUploadedIntegrationEventSubcriber implements IntegrationEventSubscriber<AssetUploadedIntegrationEvent> {
    
    private final AssetStorage assetStorage;

    public AssetUploadedIntegrationEventSubcriber(
            AssetStorage assetStorage) {
                
        this.assetStorage = assetStorage;
    }

    @Override
    public void handle(AssetUploadedIntegrationEvent event) {

    }
}
