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
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AvatarFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetDimension;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.model.profile.event.AvatarReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

@Component
public class AvatarFileStorageUploadedIntegrationEventBatchSubscriber implements IntegrationEventBatchSubscriber<AvatarFileStorageUploadedIntegrationEvent> {
    
    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AvatarFileStorageUploadedIntegrationEventBatchSubscriber(
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
    public void handle(List<AvatarFileStorageUploadedIntegrationEvent> events) {
        // 1. Exit early if the batch is empty
        if (events == null || events.isEmpty()) return;

        // 2. Deduplicate events by ProfileId, keeping only the latest event to prevent rapid-click bugs
        Map<ProfileId, AvatarFileStorageUploadedIntegrationEvent> latestEventPerProfile = events.stream()
            .collect(Collectors.toMap(
                e -> new ProfileId(Long.parseLong(e.getContext().get("profileId").toString())),
                Function.identity(),
                (existing, replacement) -> replacement 
            ));

        // 3. Extract and sort unique file hashes to prevent database deadlocks when locking
        List<String> sortedHashes = latestEventPerProfile.values().stream()
            .map(AvatarFileStorageUploadedIntegrationEvent::getFileHash)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // 4. Fetch existing assets from the database with a pessimistic lock (FOR UPDATE)
        List<Asset> existingAssets = assetRepository.findAllByFileHashInForUpdate(sortedHashes);

        // 5. Create an in-memory cache of existing assets for quick lookup
        Map<String, Asset> assetCache = existingAssets.stream()
            .collect(Collectors.toMap(Asset::getFileHash, Function.identity()));

        // 6. Initialize a map to link each Profile to its corresponding Asset
        Map<ProfileId, Asset> profileAssetMap = new HashMap<>();

        // 7. Process each deduplicated event to attach existing assets or initialize new ones
        for (AvatarFileStorageUploadedIntegrationEvent event : latestEventPerProfile.values()) {
            ProfileId profileId = new ProfileId(Long.parseLong(event.getContext().get("profileId").toString()));
            String hash = event.getFileHash();

            Asset asset = assetCache.get(hash);

            if (asset != null) {
                // Asset exists in the DB or was created earlier in this loop -> increment refCount
                asset.attached();
            } else {
                // Asset is completely new -> initialize it and add it to the cache
                asset = Asset.createNew(
                    new AssetId(timeBasedIdGenerator.generate()),
                    Namespace.valueOf(event.getNamespace()),
                    hash,
                    new AssetDimension(event.getWidth(), event.getHeight()),
                    event.getFileSize()
                );
                assetCache.put(hash, asset);
            }

            // Link the profile to the correctly resolved asset
            profileAssetMap.put(profileId, asset);
        }

        // 8. Extract and sort Profile IDs to prevent deadlocks when locking profiles
        List<ProfileId> sortedProfileIds = profileAssetMap.keySet().stream()
            .sorted(Comparator.comparing(ProfileId::getValue)) 
            .collect(Collectors.toList());
            
        // 9. Fetch profiles from the database with a pessimistic lock
        List<Profile> profiles = profileRepository.findAllByIdInForUpdate(sortedProfileIds);

        // 10. Update each profile's avatar with the new file hash
        profiles.forEach(profile -> {
            Asset asset = profileAssetMap.get(profile.getId());
            if (asset != null) {
                profile.changeAvatar(asset.getFileHash());
            }
        });

        // 11. Save all new and updated assets to the database (batch insert/update)
        assetRepository.saveAll(assetCache.values());
        
        // 12. Save all updated profiles to the database
        profileRepository.saveAll(profiles);

        // 13. Collect all domain events generated by the updated profiles
        List<DomainEvent> allDomainEvents = profiles.stream()
            .flatMap(profile -> profile.getDomainEvents().stream())
            .collect(Collectors.toList());

        // 14. Filter the domain events to isolate only the avatar replacements
        List<AvatarReplacedDomainEvent> avatarChangedEvents = avatarReplacedDomainEventFiltered(allDomainEvents);

        // 15. Wrap the domain events into integration events intended for the file storage service
        List<IntegrationEventWrapper<AssetDetachedIntegrationEvent>> integrationEvents = avatarChangedEvents.stream()
            .map(event -> new IntegrationEventWrapper<>(
                uniqueStringIdGenerator.generate(),
                event.getHashFile(),
                new AssetDetachedIntegrationEvent(event.getNamespace(), event.getHashFile())
            ))
            .collect(Collectors.toList());

        // 16. Publish the integration events to the outbox
        integrationEventPublisher.publish(integrationEvents);
    }

    // Helper method to safely filter and cast specific domain events
    private List<AvatarReplacedDomainEvent> avatarReplacedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof AvatarReplacedDomainEvent)
                .map(event -> (AvatarReplacedDomainEvent) event)
                .toList();
    }
}