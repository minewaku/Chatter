package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDeletedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;

public class AssetDeletedIntegrationEventSubcriber implements IntegrationEventSubscriber<AssetDeletedIntegrationEvent> {
    
    private final AssetStorage assetStorage;

    public AssetDeletedIntegrationEventSubcriber(
            AssetStorage assetStorage) {
                
        this.assetStorage = assetStorage;
    }

    @Override
    public void handle(AssetDeletedIntegrationEvent event) {
    }
}
