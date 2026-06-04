package com.minewaku.chatter.profile.infrastructure.cache.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.application.port.outbound.query.ProfileReadRepository;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.infrastructure.cache.ProfileReadRedisRepository;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class ProfileReadRepositoryImpl implements ProfileReadRepository {

    private final ProfileReadRedisRepository profileReadRedisRepository;
    private final ProfileRepository profileRepository;

    @Override
    @Retry(name = "transientDataAccess")
    public Optional<ProfileReadModel> findById(ProfileId id) {
        
        Optional<ProfileReadModel> cachedModel = Optional.empty();
        try {
            cachedModel = profileReadRedisRepository.findById(id.getValue().toString());
        } catch (Exception e) {
            log.warn("Redis connection error for Profile {}: {}", id.getValue(), e.getMessage());
        }

        return cachedModel.or(() -> profileRepository.findById(id)
            .map(profile -> {
                ProfileReadModel readModel = new ProfileReadModel(
                    profile.getId().getValue(),
                    profile.getUsername().getValue(),
                    profile.getDisplayName().getValue(),
                    profile.getBio().getValue(),
                    profile.getAvatarHash(),
                    profile.getBannerHash()
                );
                
                // 3. Cố gắng lưu vào Cache, nếu Cache sập cũng không làm hỏng request
                try {
                    profileReadRedisRepository.save(readModel);
                } catch (Exception e) {
                    log.warn("Lỗi lưu Redis cho Profile {}: {}", id.getValue(), e.getMessage());
                }
                
                return readModel;
            })
        );
    }
}