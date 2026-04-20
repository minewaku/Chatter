package com.minewaku.chatter.profile.application.service.command.security;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.UpdateEnablementUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UpdateEnablementApplicationService implements UpdateEnablementUseCase {

    private final ProfileRepository profileRepository;

    public UpdateEnablementApplicationService(
            ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
    }   

    @Override
	@Retry(name = "transientDataAccess")
    @CacheEvict(value = "profiles", key = "#command.profileId().value()")
    @Transactional
    public Void handle(UpdateEnablementUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (profile.updateEnablement(command.enablement())) {
            profileRepository.save(profile);
        }
        return null;
    }
    
}
