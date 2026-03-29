package com.minewaku.chatter.profile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

@Component
public interface JpaProfileRepository extends JpaRepository<Profile, ProfileId> {
    
}
