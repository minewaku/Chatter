package com.minewaku.chatter.adapter.db.redis.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.redis.helper.Prefix;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.value.Email;

@Repository
public class RedisConfirmationTokenRepository implements ConfirmationTokenRepository {

    private final RedisTemplate<String, ConfirmationToken> redisTemplateConfirmationToken;
    private final RedisTemplate<String, String> redisTemplateString;

    public RedisConfirmationTokenRepository(
        RedisTemplate<String, ConfirmationToken> redisTemplateConfirmationToken,
        RedisTemplate<String, String> redisTemplateString
    ) {
        this.redisTemplateConfirmationToken = redisTemplateConfirmationToken;
        this.redisTemplateString = redisTemplateString;
    }

    @Override
    public Optional<ConfirmationToken> findByToken(String token) {
    	String key = Prefix.CONFIRMATION_TOKEN_VALUE.format(token);
    	
        ConfirmationToken confirmationToken = redisTemplateConfirmationToken.opsForValue().get(key);
        return Optional.ofNullable(confirmationToken);
    }

    @Override
    public ConfirmationToken save(ConfirmationToken confirmationToken) {
    	
        Duration ttl = Duration.between(Instant.now(), confirmationToken.getExpiresAt());
        String valueKey = Prefix.CONFIRMATION_TOKEN_VALUE.format(confirmationToken.getToken());
        String lookupKey = Prefix.CONFIRMATION_TOKEN_LOOKUP.format(confirmationToken.getEmail().getValue());

        if (!ttl.isNegative() && !ttl.isZero()) {
        	redisTemplateConfirmationToken.opsForValue().set(valueKey, confirmationToken, ttl);
        	redisTemplateString.opsForValue().set(lookupKey, valueKey, ttl);
        } else {
            throw new IllegalStateException("Token already expired, cannot save.");
        }

        return confirmationToken;
    }

    @Override
    public void confirmEmail(ConfirmationToken token) {
        token.verifyToken();
        save(token);
    }

    @Override
    public void deleteByEmail(Email email) {
    	String lookupKey = Prefix.CONFIRMATION_TOKEN_LOOKUP.format(email.getValue());
    	String valueKey = redisTemplateString.opsForValue().get(lookupKey);
    	
    	redisTemplateConfirmationToken.delete(valueKey);
        redisTemplateString.delete(lookupKey);
    }

	@Override
	public void deleteByToken(String token) {
		String valueKey = Prefix.CONFIRMATION_TOKEN_VALUE.format(token);
		redisTemplateConfirmationToken.delete(valueKey);
	}
}
