package com.minewaku.chatter.profile.application.port.outbound.query;

import java.util.Optional;

import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface ProfileReadRepository {
    Optional<ProfileReadModel> findById(ProfileId id);
}
