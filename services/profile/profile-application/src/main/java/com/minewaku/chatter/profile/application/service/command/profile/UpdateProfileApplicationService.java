package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.ProfileUpdatedIntegrationEvent;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UpdateProfileApplicationService implements UpdateProfileUseCase {

    private final ProfileRepository profileRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;
    
    public UpdateProfileApplicationService(
                ProfileRepository profileRepository,
                UniqueStringIdGenerator uniqueStringIdGenerator,
                OutboxStore outboxStore) {

        this.profileRepository = profileRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Retry(name = "transientDataAccess")
    @CacheEvict(value = "profiles", key = "#command.profileId().value()")
    @Transactional
    public Void handle(UpdateProfileUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

            boolean isModified = false;
            if (command.displayName() != null) {
                isModified |= profile.changeDisplayName(command.displayName());
            }
            
            if (command.bio() != null) {
                isModified |= profile.changeBio(command.bio());
            }

            if (isModified) {
                profileRepository.save(profile);
            }

        IntegrationEventWrapper<ProfileUpdatedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<ProfileUpdatedIntegrationEvent>(
            uniqueStringIdGenerator.generate(),
            profile.getId().getValue().toString(),
            new ProfileUpdatedIntegrationEvent(
                profile.getId().getValue(),
                profile.getDisplayName() != null ? profile.getDisplayName().getValue() : null,
                profile.getBio() != null ? profile.getBio().getValue() : null
            )
        );
        profileRepository.save(profile);
        integrationEventPublisher.publish(eventWrapper);
        return null;
    }
    
}
