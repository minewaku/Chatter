package com.minewaku.chatter.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisUtils {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void setValue(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    public Optional<String> getValue(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    public Long increment(String key, Duration ttl) {
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L && ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            stringRedisTemplate.expire(key, ttl);
        }
        return current;
    }
}