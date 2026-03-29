package com.minewaku.chatter.profile.infrastructure.persistence.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.JpaProfileRepository;

@Repository
public class ProfileRepositoryImpl implements ProfileRepository {
    
    private final JpaProfileRepository jpaProfileRepository;

    public ProfileRepositoryImpl(JpaProfileRepository jpaProfileRepository) {
        this.jpaProfileRepository = jpaProfileRepository;
    }

    @Override
    public void save(Profile profile) {
        jpaProfileRepository.save(profile);
    }

    @Override
    public void delete(Profile profile) {   
        jpaProfileRepository.delete(profile);
    }

    @Override
    public void deleteById(ProfileId profileId) {
        jpaProfileRepository.deleteById(profileId);
    }

    @Override
    public Optional<Profile> findById(ProfileId profileId) {
        return jpaProfileRepository.findById(profileId);
    }
}
