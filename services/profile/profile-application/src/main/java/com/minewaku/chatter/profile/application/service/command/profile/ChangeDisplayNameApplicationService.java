package com.minewaku.chatter.profile.application.service.command.profile;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.ChangeDisplayNameUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;



public class ChangeDisplayNameApplicationService implements ChangeDisplayNameUseCase {
    
    private final ProfileRepository profileRepository;

    public ChangeDisplayNameApplicationService(
                ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
    }

    @Override
    public Void handle(ChangeDisplayNameUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        profile.changeDisplayName(command.displayName());
        profileRepository.save(profile);
        
        return null;
    }
}
