package com.minewaku.chatter.profile.application.service.command.security;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.UnlockProfileUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class UnlockProfileApplicationService implements UnlockProfileUseCase {

	private final ProfileRepository profileRepository;


	public UnlockProfileApplicationService(
				ProfileRepository profileRepository) {

					
		this.profileRepository = profileRepository;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public Void handle(ProfileId profileId) {
		Profile profile = profileRepository.findById(profileId)
			.orElseThrow(() -> new EntityNotFoundException("Profile does not exist"));
		profile.unlock();

		profileRepository.save(profile);
		return null;
	}
}
