package com.minewaku.chatter.profile.infrastructure.cache.mapper;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;

@Component
public class ProfileReadMapper {
    
public ProfileReadModel entityToModel(Profile entity) {
        if (entity == null) {
            return null;
        }

        return new ProfileReadModel(
            entity.getId() != null ? entity.getId().getValue() : null,
            entity.getUsername() != null ? entity.getUsername().getValue() : null,
            entity.getDisplayName() != null ? entity.getDisplayName().getValue() : null,
            entity.getBio() != null ? entity.getBio().getValue() : null,
            entity.getAvatarHash(),
            entity.getBannerHash()
        );
    }
}
