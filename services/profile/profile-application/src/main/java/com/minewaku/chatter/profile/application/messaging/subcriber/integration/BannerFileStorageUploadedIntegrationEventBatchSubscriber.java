package com.minewaku.chatter.profile.application.messaging.subcriber.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.BannerFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.BannerReplacedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.profile.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.profile.domain.model.profile.event.BannerReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BannerFileStorageUploadedIntegrationEventBatchSubscriber implements IntegrationEventBatchSubscriber<BannerFileStorageUploadedIntegrationEvent> {
    
    private final ProfileRepository profileRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final ProcessedEventRepository processedEventRepository;
    private final IntegrationEventPublisher integrationEventPublisher;

    public BannerFileStorageUploadedIntegrationEventBatchSubscriber(
        ProfileRepository profileRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        ProcessedEventRepository processedEventRepository,
        OutboxStore outboxStore
    ) {
        this.profileRepository = profileRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.processedEventRepository = processedEventRepository;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(List<BannerFileStorageUploadedIntegrationEvent> events) {
        if (events == null || events.isEmpty()) return;

        // ----- Idempotency filter -----
        Set<String> eventIds = events.stream()
                .map(BannerFileStorageUploadedIntegrationEvent::getEventId)
                .collect(Collectors.toSet());

        Set<String> alreadyProcessed = processedEventRepository.findAllExistingIds(eventIds);

        List<BannerFileStorageUploadedIntegrationEvent> newEvents = events.stream()
                .filter(e -> !alreadyProcessed.contains(e.getEventId()))
                .toList();

        if (newEvents.isEmpty()) {
            log.debug("All {} events in batch already processed, skip", events.size());
            return;
        }


        //1. Deduplicate by ProfileId to get the latest event for each profile per each batch
        Map<ProfileId, BannerFileStorageUploadedIntegrationEvent> latestEventPerProfile = events.stream()
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
            BannerFileStorageUploadedIntegrationEvent event = latestEventPerProfile.get(profile.getId());
            
            if (event != null) {
                if (profile.changeBanner(event.getFileHash())) {
                    modifiedProfiles.add(profile);
                    
                    List<BannerReplacedDomainEvent> bannerEvents = bannerReplacedDomainEventsFiltered(profile.getDomainEvents());
                    for (BannerReplacedDomainEvent domainEvent : bannerEvents) {
                        
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

                        eventWrappers.add(new IntegrationEventWrapper<>(
                            uniqueStringIdGenerator.generate(),
                            domainEvent.getNewHashFile(),
                            new BannerReplacedIntegrationEvent(
                                domainEvent.getProfileId(),
                                domainEvent.getOldHashFile(),
                                domainEvent.getNewHashFile()
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

        // ----- Mark batch as processed -----
        List<ProcessedEventRepository.ProcessedEventRecord> processedRecords = newEvents.stream()
                .map(e -> new ProcessedEventRepository.ProcessedEventRecord(e.getEventId(), Instant.now()))
                .toList();

        processedEventRepository.saveAll(processedRecords);
    }

    List<BannerReplacedDomainEvent> bannerReplacedDomainEventsFiltered(List<DomainEvent> domainEvents) {
        return domainEvents.stream()
            .filter(e -> e instanceof BannerReplacedDomainEvent)
            .map(e -> (BannerReplacedDomainEvent) e)
            .toList();
    }
}