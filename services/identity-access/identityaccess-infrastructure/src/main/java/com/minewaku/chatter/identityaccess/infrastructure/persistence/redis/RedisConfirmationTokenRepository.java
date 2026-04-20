package com.minewaku.chatter.identityaccess.infrastructure.persistence.redis;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.helper.Prefix;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.model.ConfirmationTokenModel;

@Repository
public class RedisConfirmationTokenRepository {

    private final RedisTemplate<String, ConfirmationTokenModel> redisTemplateConfirmationToken;
    
    private final RedisTemplate<String, String> stringRedisTemplate;

    public RedisConfirmationTokenRepository(
            RedisTemplate<String, ConfirmationTokenModel> redisTemplateConfirmationToken,
            RedisTemplate<String, String> stringRedisTemplate) {
        this.redisTemplateConfirmationToken = redisTemplateConfirmationToken;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Optional<ConfirmationTokenModel> findByToken(String token) {
        String key = Prefix.CONFIRMATION_TOKEN_VALUE.format(token);

        ConfirmationTokenModel confirmationTokenDto = redisTemplateConfirmationToken.opsForValue().get(key);
        return Optional.ofNullable(confirmationTokenDto);
    }

    public ConfirmationTokenModel save(ConfirmationTokenModel confirmationTokenDto) {

        Duration ttl = Duration.between(Instant.now(), confirmationTokenDto.expiresAt());
        String valueKey = Prefix.CONFIRMATION_TOKEN_VALUE.format(confirmationTokenDto.token());
        String lookupKey = Prefix.CONFIRMATION_TOKEN_LOOKUP.format(confirmationTokenDto.email());

        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplateConfirmationToken.opsForValue().set(valueKey, confirmationTokenDto, ttl);
            stringRedisTemplate.opsForValue().set(lookupKey, valueKey, ttl);
        } else {
            throw new IllegalStateException("Token already expired, cannot save.");
        }

        return confirmationTokenDto;
    }
    

    public void deleteByEmail(String email) {
        String lookupKey = Prefix.CONFIRMATION_TOKEN_LOOKUP.format(email);
        String valueKey = stringRedisTemplate.opsForValue().get(lookupKey);

        if (valueKey != null) {
            redisTemplateConfirmationToken.delete(valueKey);
            stringRedisTemplate.delete(lookupKey);
        }
    }
}
