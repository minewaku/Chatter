package com.minewaku.chatter.profile.application.service.command.profile;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.ChangeBioUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;



public class ChangeBioApplicationService implements ChangeBioUseCase {

    private final ProfileRepository profileRepository;

    public ChangeBioApplicationService(
                ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
    }

    @Override
    public Void handle(ChangeBioUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        profile.changeBio(command.bio());
        profileRepository.save(profile);
        
        return null;
    }
    
}
