package com.minewaku.chatter.profile.infrastructure.persistence.postgresql;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface ProfileJdbcRepository extends ListCrudRepository<Profile, ProfileId> {

    @Query("SELECT * FROM profile WHERE id IN (:ids) FOR UPDATE")
    List<Profile> findAllByIdInForUpdate(@Param("ids") List<Long> ids);
}
