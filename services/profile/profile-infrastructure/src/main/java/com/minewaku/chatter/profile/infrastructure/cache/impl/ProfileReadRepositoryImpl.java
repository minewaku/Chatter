package com.minewaku.chatter.profile.infrastructure.cache.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.port.outbound.query.ProfileReadRepository;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.infrastructure.cache.ProfileReadRedisRepository;

@Component
public class ProfileReadRepositoryImpl implements ProfileReadRepository {

    private final ProfileReadRedisRepository profileReadRedisRepository;

    public ProfileReadRepositoryImpl(ProfileReadRedisRepository profileReadRedisRepository) {
        this.profileReadRedisRepository = profileReadRedisRepository;
    }

    @Override
    public Optional<ProfileReadModel> findById(ProfileId id) {
        Optional<ProfileReadModel> model = profileReadRedisRepository.findById(id.getValue().toString());
        return model;
    }
    
}
