package com.minewaku.chatter.adapter.db.redis.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.redis.helper.Prefix;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;

@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

 	private final RedisTemplate<String, RefreshToken> redisTemplateRefreshToken;

    public RedisRefreshTokenRepository(RedisTemplate<String, RefreshToken> redisTemplateRefreshToken) {
        this.redisTemplateRefreshToken = redisTemplateRefreshToken;
    }
	
	@Override
	public void save(RefreshToken refreshToken) {
		Duration ttl = Duration.between(Instant.now(), refreshToken.getExpiresAt());
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshToken.getToken());
		redisTemplateRefreshToken.opsForValue().set(key, refreshToken, ttl);
	}

	@Override
	public void update(RefreshToken refreshToken) {
		Duration ttl = Duration.between(Instant.now(), refreshToken.getExpiresAt());
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshToken.getToken());
		redisTemplateRefreshToken.opsForValue().set(key, refreshToken, ttl);
	}

	@Override
	public void delete(RefreshToken refreshToken) {
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshToken.getToken());
		redisTemplateRefreshToken.delete(key);
	}

	@Override
	public void revoke(RefreshToken refreshToken) {
		Duration ttl = Duration.between(Instant.now(), refreshToken.getExpiresAt());
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshToken.getToken());
		redisTemplateRefreshToken.opsForValue().set(key, refreshToken, ttl);
	}

	@Override
	public Optional<RefreshToken> findByToken(String token) {
	    String key = Prefix.REFRESH_TOKEN_VALUE.format(token);
	    RefreshToken refreshToken = redisTemplateRefreshToken.opsForValue().get(key);
	    return Optional.ofNullable(refreshToken);
	}

}
