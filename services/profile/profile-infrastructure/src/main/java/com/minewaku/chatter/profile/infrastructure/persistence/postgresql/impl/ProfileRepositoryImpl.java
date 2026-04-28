package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.infrastructure.cache.ProfileReadRedisRepository;
import com.minewaku.chatter.profile.infrastructure.cache.mapper.ProfileReadMapper;
import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.ProfileJdbcRepository;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@AllArgsConstructor
@Log4j2
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJdbcRepository profileJdbcRepository;
    private final ProfileReadRedisRepository profileReadRedisRepository;
    private final ProfileReadMapper profileReadMapper;

    @Override
    public void save(Profile profile) {
        Profile tp = profileJdbcRepository.save(profile);
        log.info("Saved profile: {}", tp);
        profileReadRedisRepository.save(profileReadMapper.entityToModel(profile));
    }

    @Override
    public void delete(Profile profile) {
        profileJdbcRepository.delete(profile);
    }

    @Override
    public void deleteById(ProfileId profileId) {
        profileJdbcRepository.deleteById(profileId);
    }

    @Override
    public Optional<Profile> findById(ProfileId profileId) {
        return profileJdbcRepository.findById(profileId);
    }

}
