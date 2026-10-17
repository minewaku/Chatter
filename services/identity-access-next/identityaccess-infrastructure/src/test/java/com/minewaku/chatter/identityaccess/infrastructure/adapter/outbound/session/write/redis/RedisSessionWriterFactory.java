package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.SessionProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Lets tests in other packages build the write adapter, whose constructor is package-private. */
public final class RedisSessionWriterFactory {

    private RedisSessionWriterFactory() {}

    public static AuthenticationSessionRepository create(StringRedisTemplate redis, String keyPrefix) {
        return create(redis, keyPrefix, Duration.ofMinutes(15));
    }

    public static AuthenticationSessionRepository create(
            StringRedisTemplate redis, String keyPrefix, Duration accessTokenTtl) {
        return new RedisAuthenticationSessionRepository(
                redis,
                new RedisKeyProperties(keyPrefix),
                new SessionProperties(Duration.ofDays(30), Duration.ofDays(30)),
                new JwtProperties(null, null, null, null, accessTokenTtl),
                new SessionRecordMapper());
    }
}
