package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.CreateProfileUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class CreateProfileApplicationService implements CreateProfileUseCase {
    
    private final ProfileRepository profileRepository;

    public CreateProfileApplicationService(
            ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
    }

    @Override
    @Retry(name = "transientDataAccess")
    @CacheEvict(value = "profiles", key = "#command.profileId().value()")
	@Transactional
    public Void handle(Command command) {

        Profile profile = Profile.CreateNew(
                command.profileId(),
                command.username(),
                null, null,
                command.enablement()
        );

        profileRepository.save(profile);
        return null;
    }

    
}
