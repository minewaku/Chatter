package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.throttle;

import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisScripts;
import com.minewaku.chatter.identityaccess.infrastructure.integration.resilience.ResilienceConfiguration;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.port.AttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptOutcome;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * Counts attempts in Redis, one integer per policy and subject that expires with its window. Only the SHA-256 digest
 * of a subject is part of a key, so addresses never appear in the store. Every call runs inside the session-store
 * breaker; a failed call or an open breaker is reported as {@link DependencyUnavailableException}, never as "allowed".
 */
@Repository
class RedisAttemptLimiter implements AttemptLimiter {

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> HIT = RedisScripts.load("throttle-hit.lua", List.class);

    private final StringRedisTemplate redis;
    private final CircuitBreaker breaker;
    private final String keyPrefix;

    RedisAttemptLimiter(
            StringRedisTemplate redis,
            RedisKeyProperties keys,
            @Qualifier(ResilienceConfiguration.SESSION_STORE) CircuitBreaker breaker) {
        this.redis = redis;
        this.breaker = breaker;
        this.keyPrefix = keys.keyPrefix() + "throttle:";
    }

    @Override
    public AttemptOutcome hit(AttemptPolicy policy, String subject) {
        String key = key(policy, subject);
        List<?> result = guarded(() ->
                redis.execute(HIT, List.of(key), Long.toString(policy.window().toMillis())));
        if (result == null || result.size() != 2) {
            throw new DependencyUnavailableException(holdOff(), null);
        }
        long count = ((Number) result.get(0)).longValue();
        long remainingMillis =
                Math.min(((Number) result.get(1)).longValue(), policy.window().toMillis());
        return new AttemptOutcome(count <= policy.maxAttempts(), Duration.ofMillis(Math.max(remainingMillis, 0)));
    }

    @Override
    public void clear(AttemptPolicy policy, String subject) {
        String key = key(policy, subject);
        guarded(() -> redis.delete(key));
    }

    private <T> T guarded(Supplier<T> call) {
        try {
            return breaker.executeSupplier(call);
        } catch (DependencyUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            // an open breaker (CallNotPermittedException) and a failed call are the same to the caller
            throw new DependencyUnavailableException(holdOff(), e);
        }
    }

    private Duration holdOff() {
        return Duration.ofMillis(breaker.getCircuitBreakerConfig()
                .getWaitIntervalFunctionInOpenState()
                .apply(1));
    }

    private String key(AttemptPolicy policy, String subject) {
        return keyPrefix + policy.name() + ":" + TokenDigest.sha256Hex(subject);
    }
}
