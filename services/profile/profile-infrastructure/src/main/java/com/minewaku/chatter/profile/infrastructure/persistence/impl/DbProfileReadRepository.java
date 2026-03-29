package com.minewaku.chatter.profile.infrastructure.persistence.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.profile.application.port.outbound.query.ProfileReadRepository;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;

@Repository
public class DbProfileReadRepository implements ProfileReadRepository {

    private final ProfileRepository profileRepository;

    public DbProfileReadRepository(
                ProfileRepository profileRepository) {

        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<ProfileReadModel> findById(ProfileId id) {
        return profileRepository.findById(id)
            .map(profile -> new ProfileReadModel(
                profile.getId().getValue(),
                profile.getUsername().getValue(),
                profile.getDisplayName().getValue(),
                profile.getBio().getValue(),
                profile.getAvatarHash(),
                profile.getBannerHash()
            ));
    }
    
}
