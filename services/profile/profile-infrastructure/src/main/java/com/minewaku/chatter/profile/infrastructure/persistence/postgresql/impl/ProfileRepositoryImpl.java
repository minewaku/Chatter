package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        profileReadRedisRepository.deleteById(profile.getId().toString());
    }

    @Override
    public void deleteById(ProfileId profileId) {
        profileJdbcRepository.deleteById(profileId);
        profileReadRedisRepository.deleteById(profileId.getValue().toString());
    }

    @Override
    public Optional<Profile> findById(ProfileId profileId) {
        return profileJdbcRepository.findById(profileId);
    }

    @Override
    public List<Profile> findAllByIds(List<ProfileId> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> rawIds = profileIds.stream()
            .map(ProfileId::getValue)
            .collect(Collectors.toList());

        return profileJdbcRepository.findAllByIdInForUpdate(rawIds);
    }

    @Override
    public void saveAll(Iterable<Profile> profiles) {
        if (profiles == null || !profiles.iterator().hasNext()) {
            return;
        }

        List<Profile> savedProfiles = profileJdbcRepository.saveAll(profiles);
        log.info("Saved batch of {} profiles to JDBC", savedProfiles.size());

        var redisModels = savedProfiles.stream()
            .map(profileReadMapper::entityToModel)
            .collect(Collectors.toList());

        profileReadRedisRepository.saveAll(redisModels);
    }
}