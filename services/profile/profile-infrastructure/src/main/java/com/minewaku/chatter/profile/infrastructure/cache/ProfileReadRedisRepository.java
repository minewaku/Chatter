package com.minewaku.chatter.profile.infrastructure.cache;

import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.infrastructure.cache.helper.Prefix;

@Repository
public class ProfileReadRedisRepository {

    private final RedisTemplate<String, ProfileReadModel> redisTemplateProfileReadModel;

    public ProfileReadRedisRepository(RedisTemplate<String, ProfileReadModel> redisTemplateProfileReadModel) {
        this.redisTemplateProfileReadModel = redisTemplateProfileReadModel;
    }

    public void save(ProfileReadModel profileReadModel) {
        String key = Prefix.PROFILE.format(profileReadModel.id().toString());
        redisTemplateProfileReadModel.opsForValue().set(key, profileReadModel);
    }

    public void saveAll(Iterable<ProfileReadModel> profileReadModels) {
        for (ProfileReadModel profileReadModel : profileReadModels) {
            save(profileReadModel);
        }
    }

    public void deleteById(String id) {
        String key = Prefix.PROFILE.format(id);
        redisTemplateProfileReadModel.delete(key);
    }

    public Optional<ProfileReadModel> findById(String id) {
        String key = Prefix.PROFILE.format(id);
        ProfileReadModel model = redisTemplateProfileReadModel.opsForValue().get(key);
        return Optional.ofNullable(model);
    }
}

