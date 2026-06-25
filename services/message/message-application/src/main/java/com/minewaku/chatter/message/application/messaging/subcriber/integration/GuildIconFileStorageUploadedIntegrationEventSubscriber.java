package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.GuildIconReplacedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.GuildIconFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventSubscriber;
import com.minewaku.chatter.message.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.message.domain.model.guild.event.GuildIconReplacedDomainEvent;
import com.minewaku.chatter.message.domain.model.guild.model.Guild;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GuildIconFileStorageUploadedIntegrationEventSubscriber implements IntegrationEventSubscriber<GuildIconFileStorageUploadedIntegrationEvent> {
    
    private final GuildRepository guildRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final ProcessedEventRepository processedEventRepository;
    private final IntegrationEventPublisher integrationEventPublisher;

    public GuildIconFileStorageUploadedIntegrationEventSubscriber (
        GuildRepository guildRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        ProcessedEventRepository processedEventRepository,
        OutboxStore outboxStore
    ) {
        this.guildRepository = guildRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.processedEventRepository = processedEventRepository;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(GuildIconFileStorageUploadedIntegrationEvent event) {

        // ----- Idempotency check -----
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("Event {} already processed, skip", event.getEventId());
            return;
        }

        String guildIdString = event.getContext().get("guildId").toString();
        GuildId guildId = new GuildId(Long.parseLong(guildIdString));

        Guild guild = guildRepository.findById(guildId).orElseThrow(
            () -> new EntityNotFoundException("Guild not found for id: " + guildId));
    
        List<IntegrationEventWrapper<?>> eventWrappers = new ArrayList<>();
        if(guild.changeIcon(event.getFileHash())) {
            List<GuildIconReplacedDomainEvent> domainEvents = filterEvents(guild.getDomainEvents());
            for (GuildIconReplacedDomainEvent domainEvent : domainEvents) {
                
                eventWrappers.add(new IntegrationEventWrapper<AssetAttachedIntegrationEvent>(
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

                eventWrappers.add(new IntegrationEventWrapper<GuildIconReplacedIntegrationEvent>(
                    uniqueStringIdGenerator.generate(),
                    domainEvent.getNewHashFile(),
                    new GuildIconReplacedIntegrationEvent(
                        domainEvent.getGuildId().toString(),
                        domainEvent.getOldHashFile(),
                        event.getNamespace(),
                        domainEvent.getNewHashFile(),
                        event.getContentType(),
                        event.getFileName(),
                        event.getFileSize()
                    )
                ));

                if (domainEvent.getOldHashFile() != null) {
                    eventWrappers.add(new IntegrationEventWrapper<AssetDetachedIntegrationEvent>(
                        uniqueStringIdGenerator.generate(),
                        domainEvent.getOldHashFile(),
                        new AssetDetachedIntegrationEvent(
                            event.getNamespace(), 
                            domainEvent.getOldHashFile()
                        )
                    ));

                    eventWrappers.add(new IntegrationEventWrapper<AssetDetachedIntegrationEvent>(
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

        guildRepository.save(guild);
        integrationEventPublisher.publish(eventWrappers);

        processedEventRepository.save(event.getEventId(), Instant.now());
    } 

    List<GuildIconReplacedDomainEvent> filterEvents(List<DomainEvent> domainEvents) {
        return domainEvents.stream()
            .filter(e -> e instanceof GuildIconReplacedDomainEvent)
            .map(e -> (GuildIconReplacedDomainEvent) e)
            .toList();
    }
}
