package com.minewaku.chatter.identityaccess.infrastructure.cache;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.helper.Prefix;

@Repository
public class RedisSessionReadRepository {

    private final RedisTemplate<String, SessionReadModel> redisTemplateSessionReadModel;
    private final RedisTemplate<String, String> stringRedisTemplate;

    public RedisSessionReadRepository(
            RedisTemplate<String, SessionReadModel> redisTemplateSessionReadModel,
            RedisTemplate<String, String> stringRedisTemplate) {

        this.redisTemplateSessionReadModel = redisTemplateSessionReadModel;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    
    public void save(SessionReadModel sessionReadModel) {
        Instant now = Instant.now();
        Instant expiresAt = sessionReadModel.expiresAt();
        if (expiresAt.isBefore(now) || expiresAt.equals(now)) {
            throw new IllegalStateException("Session already expired, cannot save.");
        }

        String valueKey = Prefix.SESSION_VALUE.format(sessionReadModel.sessionId());
        String lookupKey = Prefix.SESSION_LOOKUP.format(sessionReadModel.userId().toString()); 

        redisTemplateSessionReadModel.opsForValue().set(valueKey, sessionReadModel);
        redisTemplateSessionReadModel.expireAt(valueKey, expiresAt);

        stringRedisTemplate.opsForValue().set(lookupKey, valueKey);
        stringRedisTemplate.expireAt(lookupKey, expiresAt);
    }

    public Optional<SessionReadModel> findById(String id) {
        String key = Prefix.SESSION_VALUE.format(id);

        SessionReadModel sessionReadModel = redisTemplateSessionReadModel.opsForValue().get(key);
        return Optional.ofNullable(sessionReadModel);
    }

    public Set<SessionReadModel> findAllSessionsByUserId(String userId) {
        String lookupKey = Prefix.SESSION_LOOKUP.format(userId);
        String valueKey = stringRedisTemplate.opsForValue().get(lookupKey);

        if (valueKey != null) {
            SessionReadModel sessionReadModel = redisTemplateSessionReadModel.opsForValue().get(valueKey);
            if (sessionReadModel != null) {
                return Set.of(sessionReadModel);
            }
        }

        return Set.of();
    }
}
