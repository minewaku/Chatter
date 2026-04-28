package com.minewaku.chatter.profile.infrastructure.persistence.postgresql;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface ProfileJdbcRepository extends ListCrudRepository<Profile, ProfileId> {

}
