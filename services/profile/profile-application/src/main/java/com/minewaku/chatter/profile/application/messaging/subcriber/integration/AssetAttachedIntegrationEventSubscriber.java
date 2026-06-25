package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.PersistFileStorageIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.profile.domain.model.asset.model.Asset;
import com.minewaku.chatter.profile.domain.model.asset.model.AssetId;
import com.minewaku.chatter.profile.domain.model.asset.model.AssetIdentity;
import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;
import com.minewaku.chatter.profile.domain.model.asset.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AssetAttachedIntegrationEventSubscriber implements IntegrationEventSubscriber<AssetAttachedIntegrationEvent> {
    
    private final AssetRepository assetRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final ProcessedEventRepository processedEventRepository;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AssetAttachedIntegrationEventSubscriber(
        AssetRepository assetRepository,
        TimeBasedIdGenerator timeBasedIdGenerator,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        ProcessedEventRepository processedEventRepository,
        OutboxStore outboxStore
    ) {
        this.assetRepository = assetRepository;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.processedEventRepository = processedEventRepository;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Retry(name = "transientDataAccess")
    @Transactional
    public void handle(AssetAttachedIntegrationEvent event) {
        if(processedEventRepository.existsById(event.getEventId())) {
            log.info("Event with ID {} has already been processed. Skipping.", event.getEventId());
            return;
        }

        AssetIdentity assetIdentity = new AssetIdentity(Namespace.fromValue(event.getNamespace()), event.getFileHash());
        Optional<Asset> assetOpt = assetRepository.findByAssetIdentity(assetIdentity);
        
        Asset asset;
        boolean isNewAsset = false;

        if (assetOpt.isPresent()) {
            asset = assetOpt.get();
            asset.attached();
        } else {
            asset = Asset.createNew(
                new AssetId(timeBasedIdGenerator.generate()),
                assetIdentity.getNamespace(),
                assetIdentity.getFileHash(),
                event.getContentType(),
                event.getFileName(),
                event.getFileSize()
            );
            isNewAsset = true;
        }

        assetRepository.save(asset);

        if (isNewAsset) {
            PersistFileStorageIntegrationEvent createdEvent = new PersistFileStorageIntegrationEvent(
                asset.getId().getValue(),
                assetIdentity.getNamespace().name(),
                assetIdentity.getFileHash()
            );

            IntegrationEventWrapper<PersistFileStorageIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
                uniqueStringIdGenerator.generate(),
                asset.getId().getValue().toString(),
                createdEvent
            );

            integrationEventPublisher.publish(List.of(eventWrapper));
        }

        processedEventRepository.save(
            event.getEventId(), 
            Instant.now());
    }
}
