package com.minewaku.chatter.profile.domain.model.profile.repository;

import java.util.Optional;

import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface ProfileRepository {
    void save(Profile profile);
    void delete(Profile profile);
    void deleteById(ProfileId profileId);
    Optional<Profile> findById(ProfileId profileId);
}
