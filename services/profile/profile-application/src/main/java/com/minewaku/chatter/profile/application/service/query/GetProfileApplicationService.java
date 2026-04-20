package com.minewaku.chatter.profile.application.service.query;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.query.GetProfileUseCase;
import com.minewaku.chatter.profile.application.port.outbound.query.ProfileReadRepository;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class GetProfileApplicationService implements GetProfileUseCase {

    private final ProfileReadRepository profileCacheRepository;

    public GetProfileApplicationService(ProfileReadRepository profileCacheRepository) {
        this.profileCacheRepository = profileCacheRepository;
    }

    @Override
    @Cacheable(value = "profile", key = "#request.value()")
    @Retry(name = "transientDataAccess")
    public ProfileReadModel handle(ProfileId request) {
        ProfileReadModel model = profileCacheRepository.findById(request)
            .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
        return model;
    }
}
