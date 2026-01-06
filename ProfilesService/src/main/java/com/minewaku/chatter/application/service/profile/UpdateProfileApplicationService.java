package com.minewaku.chatter.application.service.profile;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.command.profile.UpdateProfileCommand;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.profile.UpdateProfileUseCase;
import com.minewaku.chatter.domain.port.out.repository.ProfileRepository;

public class UpdateProfileApplicationService implements UpdateProfileUseCase {
    
    private final ProfileRepository profileRepository;

    public UpdateProfileApplicationService(
            ProfileRepository profileRepository) {
        
        this.profileRepository = profileRepository;
    }

    @Override
    public Void handle(UpdateProfileCommand command) {
        User user = profileRepository.findActivatedByUserId(command.userId())
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        user.setDisplayName(command.displayName());
        user.setBio(command.bio());

        profileRepository.updateUser(user);
        return null;
    }
}
