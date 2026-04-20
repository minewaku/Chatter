package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UpdateProfileApplicationService implements UpdateProfileUseCase {

    private final ProfileRepository profileRepository;

    public UpdateProfileApplicationService(
                ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
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

        profileRepository.save(profile);
        return null;
    }
    
}
