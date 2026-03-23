package com.minewaku.chatter.identityaccess.infrastructure.cache;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;

import com.minewaku.chatter.identityaccess.infrastructure.cache.dto.SessionCacheDto;
import com.minewaku.chatter.identityaccess.infrastructure.cache.helper.Prefix;

public class RedisSessionRepository {

    private final RedisTemplate<String, SessionCacheDto> redisTemplateSession;
    private final RedisTemplate<String, String> redisTemplateString;

    public RedisSessionRepository(
            RedisTemplate<String, SessionCacheDto> redisTemplateSession,
            RedisTemplate<String, String> redisTemplateString) {

        this.redisTemplateSession = redisTemplateSession;
        this.redisTemplateString = redisTemplateString;
    }
    
    public void save(SessionCacheDto sessionCacheDto) {
        Instant now = Instant.now();
        Instant expiresAt = sessionCacheDto.expiresAt();
        if (expiresAt.isBefore(now) || expiresAt.equals(now)) {
            throw new IllegalStateException("Session already expired, cannot save.");
        }

        String valueKey = Prefix.SESSION_VALUE.format(sessionCacheDto.sessionId());
        String lookupKey = Prefix.SESSION_LOOKUP.format(sessionCacheDto.userId().toString()); 

        redisTemplateSession.opsForValue().set(valueKey, sessionCacheDto);
        redisTemplateSession.expireAt(valueKey, expiresAt);

        redisTemplateString.opsForValue().set(lookupKey, valueKey);
        redisTemplateString.expireAt(lookupKey, expiresAt);
    }

    public Optional<SessionCacheDto> findById(String id) {
        String key = Prefix.SESSION_VALUE.format(id);

        SessionCacheDto sessionCacheDto = redisTemplateSession.opsForValue().get(key);
        return Optional.ofNullable(sessionCacheDto);
    }

    
}
