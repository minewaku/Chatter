package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.BannerFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetDimension;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.model.profile.event.BannerReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

@Component
public class BannerFileStorageUploadedIntegrationEventBatchSubscriber implements IntegrationEventBatchSubscriber<BannerFileStorageUploadedIntegrationEvent> {
    
    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public BannerFileStorageUploadedIntegrationEventBatchSubscriber(
        AssetRepository assetRepository,
        ProfileRepository profileRepository,
        TimeBasedIdGenerator timeBasedIdGenerator,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {

        this.assetRepository = assetRepository;
        this.profileRepository = profileRepository;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(List<BannerFileStorageUploadedIntegrationEvent> events) {
        if (events == null || events.isEmpty()) return;

        Map<ProfileId, BannerFileStorageUploadedIntegrationEvent> latestEventPerProfile = events.stream()
            .collect(Collectors.toMap(
                e -> new ProfileId(Long.parseLong(e.getContext().get("profileId").toString())),
                Function.identity(),
                (existing, replacement) -> replacement 
            ));

        List<String> sortedHashes = latestEventPerProfile.values().stream()
            .map(BannerFileStorageUploadedIntegrationEvent::getFileHash)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        List<Asset> existingAssets = assetRepository.findAllByFileHashInForUpdate(sortedHashes);

        Map<String, Asset> assetCache = existingAssets.stream()
            .collect(Collectors.toMap(Asset::getFileHash, Function.identity()));

        Map<ProfileId, Asset> profileAssetMap = new HashMap<>();

        for (BannerFileStorageUploadedIntegrationEvent event : latestEventPerProfile.values()) {
            ProfileId profileId = new ProfileId(Long.parseLong(event.getContext().get("profileId").toString()));
            String hash = event.getFileHash();

            Asset asset = assetCache.get(hash);

            if (asset != null) {
                asset.attached();
            } else {
                asset = Asset.createNew(
                    new AssetId(timeBasedIdGenerator.generate()),
                    Namespace.valueOf(event.getNamespace()),
                    hash,
                    new AssetDimension(event.getWidth(), event.getHeight()),
                    event.getFileSize()
                );
                assetCache.put(hash, asset);
            }

            profileAssetMap.put(profileId, asset);
        }

        List<ProfileId> sortedProfileIds = profileAssetMap.keySet().stream()
            .sorted(Comparator.comparing(ProfileId::getValue))
            .collect(Collectors.toList());
            
        List<Profile> profiles = profileRepository.findAllByIdInForUpdate(sortedProfileIds);

        profiles.forEach(profile -> {
            Asset asset = profileAssetMap.get(profile.getId());
            if (asset != null) {
                profile.changeBanner(asset.getFileHash());
            }
        });

        assetRepository.saveAll(assetCache.values());
        profileRepository.saveAll(profiles);

        List<DomainEvent> allDomainEvents = profiles.stream()
            .flatMap(profile -> profile.getDomainEvents().stream())
            .collect(Collectors.toList());

        List<BannerReplacedDomainEvent> bannerChangedEvents = bannerReplacedDomainEventFiltered(allDomainEvents);

        List<IntegrationEventWrapper<AssetDetachedIntegrationEvent>> integrationEvents = bannerChangedEvents.stream()
            .map(event -> new IntegrationEventWrapper<>(
                uniqueStringIdGenerator.generate(),
                event.getHashFile(),
                new AssetDetachedIntegrationEvent(event.getNamespace(), event.getHashFile())
            ))
            .collect(Collectors.toList());

        integrationEventPublisher.publish(integrationEvents);
    }

    private List<BannerReplacedDomainEvent> bannerReplacedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof BannerReplacedDomainEvent)
                .map(event -> (BannerReplacedDomainEvent) event)
                .toList();
    }
}