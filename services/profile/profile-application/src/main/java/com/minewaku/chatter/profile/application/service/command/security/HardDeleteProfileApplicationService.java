package com.minewaku.chatter.profile.application.service.command.security;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.HardDeleteProfileUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class HardDeleteProfileApplicationService implements HardDeleteProfileUseCase {
	
	private final ProfileRepository profileRepository;
	
	
	public HardDeleteProfileApplicationService(
			ProfileRepository profileRepository) {
		
		this.profileRepository = profileRepository;
	}
	
	
    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
    public Void handle(ProfileId profileId) {
		Optional<Profile> profileOpt = profileRepository.findById(profileId);
		if (profileOpt.isEmpty()) return null;

        profileRepository.deleteById(profileId);
        return null;
	}
}
