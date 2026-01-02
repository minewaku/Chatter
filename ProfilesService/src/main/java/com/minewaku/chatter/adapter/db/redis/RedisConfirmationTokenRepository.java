package com.minewaku.chatter.adapter.db.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.redis.dto.ConfirmationTokenDto;
import com.minewaku.chatter.adapter.db.redis.helper.Prefix;

@Repository
public class RedisConfirmationTokenRepository {

    private final RedisTemplate<String, ConfirmationTokenDto> redisTemplateConfirmationToken;
    private final RedisTemplate<String, String> redisTemplateString;

    public RedisConfirmationTokenRepository(
            RedisTemplate<String, ConfirmationTokenDto> redisTemplateConfirmationToken,
            RedisTemplate<String, String> redisTemplateString) {

        this.redisTemplateConfirmationToken = redisTemplateConfirmationToken;
        this.redisTemplateString = redisTemplateString;
    }

    public Optional<ConfirmationTokenDto> findByToken(String token) {
        String key = Prefix.CONFIRMATION_TOKEN_VALUE.format(token);

        ConfirmationTokenDto confirmationTokenDto = redisTemplateConfirmationToken.opsForValue().get(key);
        return Optional.ofNullable(confirmationTokenDto);
    }

    public ConfirmationTokenDto save(ConfirmationTokenDto confirmationTokenDto) {

        Duration ttl = Duration.between(Instant.now(), confirmationTokenDto.expiresAt());
        String valueKey = Prefix.CONFIRMATION_TOKEN_VALUE.format(confirmationTokenDto.token());
        String lookupKey = Prefix.CONFIRMATION_TOKEN_LOOKUP.format(confirmationTokenDto.email());

        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplateConfirmationToken.opsForValue().set(valueKey, confirmationTokenDto, ttl);
            redisTemplateString.opsForValue().set(lookupKey, valueKey, ttl);
        } else {
            throw new IllegalStateException("Token already expired, cannot save.");
        }

        return confirmationTokenDto;
    }

    public void deleteByEmail(String email) {
        String lookupKey = Prefix.CONFIRMATION_TOKEN_LOOKUP.format(email);
        String valueKey = redisTemplateString.opsForValue().get(lookupKey);

        if (valueKey != null) {
            redisTemplateConfirmationToken.delete(valueKey);
            redisTemplateString.delete(lookupKey);
        }
    }

    public void deleteByToken(String token) {
        String valueKey = Prefix.CONFIRMATION_TOKEN_VALUE.format(token);
        redisTemplateConfirmationToken.delete(valueKey);
    }
}
