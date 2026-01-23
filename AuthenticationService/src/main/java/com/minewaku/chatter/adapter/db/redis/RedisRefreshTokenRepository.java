package com.minewaku.chatter.adapter.db.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.redis.dto.RefreshTokenDto;
import com.minewaku.chatter.adapter.db.redis.helper.Prefix;

@Repository
public class RedisRefreshTokenRepository {

	private final RedisTemplate<String, RefreshTokenDto> redisTemplateRefreshTokenDto;

	public RedisRefreshTokenRepository(RedisTemplate<String, RefreshTokenDto> redisTemplateRefreshTokenDto) {
		this.redisTemplateRefreshTokenDto = redisTemplateRefreshTokenDto;
	}

	public void save(RefreshTokenDto refreshTokenDto) {
		Duration ttl = Duration.between(Instant.now(), refreshTokenDto.expiresAt());
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshTokenDto.token());
		redisTemplateRefreshTokenDto.opsForValue().set(key, refreshTokenDto, ttl);
	}

	public void update(RefreshTokenDto refreshTokenDto) {
		Duration ttl = Duration.between(Instant.now(), refreshTokenDto.expiresAt());
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshTokenDto.token());
		redisTemplateRefreshTokenDto.opsForValue().set(key, refreshTokenDto, ttl);
	}

	public void revoke(RefreshTokenDto refreshTokenDto) {
		Duration ttl = Duration.between(Instant.now(), refreshTokenDto.expiresAt());
		String key = Prefix.REFRESH_TOKEN_VALUE.format(refreshTokenDto.token());
		redisTemplateRefreshTokenDto.opsForValue().set(key, refreshTokenDto, ttl);
	}
	
	public void deleteByToken(String token) {
		String key = Prefix.REFRESH_TOKEN_VALUE.format(token);
		redisTemplateRefreshTokenDto.delete(key);
	}

	public Optional<RefreshTokenDto> findByToken(String token) {
		String key = Prefix.REFRESH_TOKEN_VALUE.format(token);
		RefreshTokenDto refreshTokenDto = redisTemplateRefreshTokenDto.opsForValue().get(key);
		return Optional.ofNullable(refreshTokenDto);
	}
}
