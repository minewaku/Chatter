package com.minewaku.chatter.profile.application.service.command.security;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.SoftDeleteProfileUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class SoftDeleteProfileApplicationService implements SoftDeleteProfileUseCase {
	
	private final ProfileRepository profileRepository;
	private final AssetRepository assetRepository;
	private final AssetStorage assetStorage;
	

	public SoftDeleteProfileApplicationService(
				ProfileRepository profileRepository,
				AssetRepository assetRepository,
				AssetStorage assetStorage) {

		this.profileRepository = profileRepository;
		this.assetRepository = assetRepository;
		this.assetStorage = assetStorage;
	}


    @Override
	@Retry(name = "transientDataAccess")
	@CacheEvict(value = "profiles", key = "#command.profileId().value()")
	@Transactional
    public Void handle(Command command) {
		Profile profile = profileRepository.findById(command.profileId())
			.orElseThrow(() -> new EntityNotFoundException("Profile does not exist"));

		String hashAvatar = profile.getAvatarHash();
		String hashBanner = profile.getBannerHash();


		if (profile.softDelete(command.username(), command.enablement())) {
			profileRepository.save(profile);

			if(hashAvatar != null) {
				assetRepository.deleteByFileHash(hashAvatar);
			}
			if(hashBanner != null) {
				assetRepository.deleteByFileHash(hashBanner);
			}
		}
		assetStorage.delete(hashAvatar);
		assetStorage.delete(hashBanner);
		
        return null;
	}
}
