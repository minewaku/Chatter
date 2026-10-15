package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.SessionProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisScripts;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * Source of truth for refresh sessions. Every conditional change is one Lua script, so concurrent callers cannot
 * interleave. Keys expire at session expiry plus the configured retention so expired and revoked sessions stay
 * readable for that long.
 */
@Repository
public class RedisAuthenticationSessionRepository implements AuthenticationSessionRepository {

    private static final DefaultRedisScript<Long> CREATE = RedisScripts.load("session-create.lua", Long.class);
    private static final DefaultRedisScript<Long> TOUCH = RedisScripts.load("session-touch.lua", Long.class);
    private static final DefaultRedisScript<Long> REVOKE_ONE = RedisScripts.load("session-revoke-one.lua", Long.class);
    private static final DefaultRedisScript<Long> REVOKE_MANY =
            RedisScripts.load("session-revoke-many.lua", Long.class);

    private final StringRedisTemplate redis;
    private final SessionRecordMapper mapper;
    private final long retentionMillis;
    private final String sessionKeyPrefix;
    private final String tokenKeyPrefix;
    private final String userKeyPrefix;

    RedisAuthenticationSessionRepository(
            StringRedisTemplate redis,
            RedisKeyProperties keys,
            SessionProperties sessions,
            SessionRecordMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
        this.retentionMillis = sessions.retention().toMillis();
        this.sessionKeyPrefix = keys.keyPrefix() + "session:";
        this.tokenKeyPrefix = keys.keyPrefix() + "session:token:";
        this.userKeyPrefix = keys.keyPrefix() + "session:user:";
    }

    @Override
    public void create(AuthenticationSession session, String rawRefreshToken) {
        String digest = TokenDigest.sha256Hex(rawRefreshToken);
        SessionRecord record = mapper.toRecord(session, digest);

        List<Object> args = new ArrayList<>();
        args.add(record.sessionId());
        args.add(digest);
        args.add(Long.toString(record.issuedAt()));
        args.add(Long.toString(RedisTime.toMillisFloor(record.expiresAt()) + retentionMillis));
        record.toHash().forEach((field, value) -> {
            args.add(field);
            args.add(value);
        });

        redis.execute(
                CREATE,
                List.of(
                        sessionKeyPrefix + record.sessionId(),
                        tokenKeyPrefix + digest,
                        userKeyPrefix + record.userId()),
                args.toArray());
    }

    @Override
    public Optional<AuthenticationSession> findByPresentedToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        String sessionId = redis.opsForValue().get(tokenKeyPrefix + TokenDigest.sha256Hex(rawRefreshToken));
        return sessionId == null ? Optional.empty() : findBySessionId(sessionId);
    }

    @Override
    public Optional<AuthenticationSession> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        Map<Object, Object> entries = redis.opsForHash().entries(sessionKeyPrefix + sessionId);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> hash = new HashMap<>();
        entries.forEach((field, value) -> hash.put((String) field, (String) value));
        return Optional.of(mapper.toModel(SessionRecord.fromHash(sessionId, hash)));
    }

    @Override
    public boolean touchIfUsable(String sessionId, Instant now) {
        Long result = redis.execute(TOUCH, List.of(sessionKeyPrefix + sessionId), micros(now));
        return result != null && result == 1L;
    }

    @Override
    public boolean revokeBySessionId(UserId userId, String sessionId, Instant now) {
        Long result = redis.execute(
                REVOKE_ONE, List.of(sessionKeyPrefix + sessionId), Long.toString(userId.value()), micros(now));
        return result != null && result == 1L;
    }

    @Override
    public int revokeOtherSessions(UserId userId, String currentSessionId, Instant now) {
        return revokeMany(userId, currentSessionId, now);
    }

    @Override
    public int revokeAllByUserId(UserId userId, Instant now) {
        return revokeMany(userId, "", now);
    }

    private int revokeMany(UserId userId, String keepSessionId, Instant now) {
        Long result = redis.execute(
                REVOKE_MANY, List.of(userKeyPrefix + userId.value()), sessionKeyPrefix, keepSessionId, micros(now));
        return result == null ? 0 : result.intValue();
    }

    private static String micros(Instant instant) {
        return Long.toString(RedisTime.toMicros(instant));
    }
}
