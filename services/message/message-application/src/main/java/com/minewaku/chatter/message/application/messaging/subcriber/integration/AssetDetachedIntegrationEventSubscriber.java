package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.DeleteFileStorageIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.message.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.message.domain.model.asset.event.AssetOrphanedDomainEvent;
import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.asset.model.AssetIdentity;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.model.asset.repository.AssetRepository;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AssetDetachedIntegrationEventSubscriber implements IntegrationEventSubscriber<AssetDetachedIntegrationEvent> {
    
    private final AssetRepository assetRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final ProcessedEventRepository processedEventRepository;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AssetDetachedIntegrationEventSubscriber(
        AssetRepository assetRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        ProcessedEventRepository processedEventRepository,
        OutboxStore outboxStore
    ) {
        this.assetRepository = assetRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.processedEventRepository = processedEventRepository;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Retry(name = "transientDataAccess")
    @Transactional
    public void handle(AssetDetachedIntegrationEvent event) {
        if(processedEventRepository.existsById(event.getEventId())) {
            log.info("Event with ID {} has already been processed. Skipping.", event.getEventId());
            return;
        }

        AssetIdentity assetIdentity = new AssetIdentity(Namespace.fromValue(event.getNamespace()), event.getFileHash());
        Optional<Asset> assetOpt = assetRepository.findByAssetIdentity(assetIdentity);
        
        if (assetOpt.isEmpty()) {
            return;
        }

        Asset asset = assetOpt.get();
        asset.detached();

        List<AssetOrphanedDomainEvent> assetOrphanedDomainEvents = assetOrphanedDomainEventFiltered(asset.getDomainEvents());

        if (!assetOrphanedDomainEvents.isEmpty()) {
            DeleteFileStorageIntegrationEvent integrationEvent = new DeleteFileStorageIntegrationEvent(
                event.getFileHash(),
                event.getNamespace()
            );

            IntegrationEventWrapper<DeleteFileStorageIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
                uniqueStringIdGenerator.generate(),
                asset.getId().getValue().toString(),
                integrationEvent
            );

            integrationEventPublisher.publish(eventWrapper);
            assetRepository.delete(asset);
        } else {
            assetRepository.save(asset);
        }

        processedEventRepository.save(
            event.getEventId(), 
            Instant.now());
    }

    private List<AssetOrphanedDomainEvent> assetOrphanedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof AssetOrphanedDomainEvent)
                .map(event -> (AssetOrphanedDomainEvent) event)
                .toList();
    }
}
