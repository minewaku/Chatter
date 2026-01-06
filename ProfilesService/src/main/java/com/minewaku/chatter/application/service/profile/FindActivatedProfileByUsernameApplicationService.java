package com.minewaku.chatter.application.service.profile;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.profile.FindActivatedProfileByUsernameUseCase;
import com.minewaku.chatter.domain.port.out.repository.ProfileRepository;
import com.minewaku.chatter.domain.value.Username;

public class FindActivatedProfileByUsernameApplicationService implements FindActivatedProfileByUsernameUseCase {
    
    private final ProfileRepository profileRepository;

    public FindActivatedProfileByUsernameApplicationService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public User handle(Username username) {
        return profileRepository.findActivatedByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));
    }
}
