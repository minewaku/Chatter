package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.cache.annotation.CacheEvict;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

public class UpdateProfileApplicationService implements UpdateProfileUseCase {

    private final ProfileRepository profileRepository;

    public UpdateProfileApplicationService(
                ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
    }

    @Override
    @CacheEvict(value = "profile", key = "#command.profileId()")
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
