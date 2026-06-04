package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AvatarFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.profile.domain.model.profile.event.AvatarReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

@Component
public class AvatarFileStorageUploadedIntegrationEventBatchSubscriber implements IntegrationEventBatchSubscriber<AvatarFileStorageUploadedIntegrationEvent> {
    
    private final ProfileRepository profileRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AvatarFileStorageUploadedIntegrationEventBatchSubscriber(
        ProfileRepository profileRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {
        this.profileRepository = profileRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(List<AvatarFileStorageUploadedIntegrationEvent> events) {
        if (events == null || events.isEmpty()) return;

        //1. Deduplicate by ProfileId to get the latest event for each profile per each batch
        Map<ProfileId, AvatarFileStorageUploadedIntegrationEvent> latestEventPerProfile = events.stream()
            .collect(Collectors.toMap(
                e -> new ProfileId(Long.parseLong(e.getContext().get("profileId").toString())), 
                Function.identity(), 
                (existing, replacement) -> replacement 
            ));


        // 2. Sorted ProfileIds to ensure consistent locking order (dead lock)
        List<ProfileId> sortedProfileIds = latestEventPerProfile.keySet().stream()
            .sorted(Comparator.comparing(ProfileId::getValue)) 
            .collect(Collectors.toList());
            

        // 3. Fetch profiles with Pessimistic Lock
        List<Profile> profiles = profileRepository.findAllByIds(sortedProfileIds);
        List<Profile> modifiedProfiles = new ArrayList<>();
        List<IntegrationEventWrapper<?>> eventWrappers = new ArrayList<>();
        

        // 4. handle update and collect domain events
        profiles.forEach(profile -> {
            AvatarFileStorageUploadedIntegrationEvent event = latestEventPerProfile.get(profile.getId());
            
            if (event != null) {
                if (profile.changeAvatar(event.getFileHash())) {
                    modifiedProfiles.add(profile);
                    
                    List<AvatarReplacedDomainEvent> avatarEvents = avatarReplacedDomainEventsFiltered(profile.getDomainEvents());
                    for (AvatarReplacedDomainEvent domainEvent : avatarEvents) {
                        
                        eventWrappers.add(new IntegrationEventWrapper<>(
                            uniqueStringIdGenerator.generate(),
                            domainEvent.getNewHashFile(),
                            new AssetAttachedIntegrationEvent(
                                event.getNamespace(), 
                                domainEvent.getNewHashFile(),
                                event.getContentType(),
                                event.getFileName(),
                                event.getFileSize()
                            )
                        ));

                        if (domainEvent.getOldHashFile() != null) {
                            eventWrappers.add(new IntegrationEventWrapper<>(
                                uniqueStringIdGenerator.generate(),
                                domainEvent.getOldHashFile(),
                                new AssetDetachedIntegrationEvent(
                                    event.getNamespace(), 
                                    domainEvent.getOldHashFile()
                                )
                            ));
                        }
                    }
                }
            }
        });


        // 5. save data
        if (!modifiedProfiles.isEmpty()) {
            profileRepository.saveAll(modifiedProfiles);
        }

        if (!eventWrappers.isEmpty()) {
            integrationEventPublisher.publish(eventWrappers);
        }
    }

    List<AvatarReplacedDomainEvent> avatarReplacedDomainEventsFiltered(List<DomainEvent> domainEvents) {
        return domainEvents.stream()
            .filter(e -> e instanceof AvatarReplacedDomainEvent)
            .map(e -> (AvatarReplacedDomainEvent) e)
            .toList();
    }
}