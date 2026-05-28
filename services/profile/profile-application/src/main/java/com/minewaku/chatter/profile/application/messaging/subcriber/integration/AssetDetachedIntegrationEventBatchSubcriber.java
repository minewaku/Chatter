package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

@Component
//recheck: AssetDetachedIntegrationEvent are published well but this class unable to handle the right logic
public class AssetDetachedIntegrationEventBatchSubcriber implements IntegrationEventBatchSubscriber<AssetDetachedIntegrationEvent> {
    
    private final AssetRepository assetRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AssetDetachedIntegrationEventBatchSubcriber(
        AssetRepository assetRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {
        this.assetRepository = assetRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(List<AssetDetachedIntegrationEvent> events) {
        List<String> fileHashes = events.stream()
            .map(AssetDetachedIntegrationEvent::getFileHash)
            .toList();

        List<Asset> assetsToDelete = new ArrayList<>();
        List<Asset> assets = assetRepository.findAllByFileHashInForUpdate(fileHashes);
        assets.stream().forEach(
            asset -> {
                if (asset.isOrphaned()) {
                    assetsToDelete.add(asset);
                }       
            }
        );

        List<AssetId> assetIdsToDelete = assetsToDelete.stream()
            .map(Asset::getId)
            .toList();


        List<IntegrationEventWrapper<AssetDetachedIntegrationEvent>> eventWrappers = new ArrayList<>();
        assetsToDelete.stream().forEach(
            asset -> {
                String eventId = uniqueStringIdGenerator.generate();
                IntegrationEventWrapper<AssetDetachedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
                    eventId,
                    asset.getFileHash(),
                    new AssetDetachedIntegrationEvent(
                        asset.getNamespace().name(), 
                        asset.getFileHash())
                );

                eventWrappers.add(eventWrapper);
            }
        );

        integrationEventPublisher.publish(eventWrappers);
        assetRepository.deleteAllByIdIn(assetIdsToDelete);
    }
}
