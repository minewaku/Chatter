package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.emailchange.write.redis;

import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisScripts;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.PendingEmailChangeRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * Source of truth for pending email changes. It has its own key family so a registration token can never complete an
 * email change, and the reverse. Only the SHA-256 digest of a token is used as a key, and the entry expires on its
 * own at the token's expiry. The save and invalidate scripts are shared with the registration tokens.
 */
@Repository
public class RedisPendingEmailChangeRepository implements PendingEmailChangeRepository {

    private static final DefaultRedisScript<Long> SAVE = RedisScripts.load("verification-save.lua", Long.class);
    private static final DefaultRedisScript<Long> INVALIDATE =
            RedisScripts.load("verification-invalidate-for-user.lua", Long.class);

    private final StringRedisTemplate redis;
    private final PendingEmailChangeRecordMapper mapper;
    private final String tokenKeyPrefix;
    private final String userKeyPrefix;

    RedisPendingEmailChangeRepository(
            StringRedisTemplate redis, RedisKeyProperties keys, PendingEmailChangeRecordMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
        this.tokenKeyPrefix = keys.keyPrefix() + "email-change:token:";
        this.userKeyPrefix = keys.keyPrefix() + "email-change:user:";
    }

    @Override
    public void save(PendingEmailChange change) {
        PendingEmailChangeRecord record = mapper.toRecord(change);
        String digest = TokenDigest.sha256Hex(change.rawToken());
        List<Object> args = new ArrayList<>();
        args.add(Long.toString(RedisTime.toMillisFloor(record.expiresAt())));
        args.add(digest);
        args.add(record.confirmedAt() == null ? "1" : "0");
        record.toHash().forEach((field, value) -> {
            args.add(field);
            args.add(value);
        });
        redis.execute(SAVE, List.of(tokenKeyPrefix + digest, userKeyPrefix + record.userId()), args.toArray());
    }

    @Override
    public Optional<PendingEmailChange> findByPresentedToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Map<Object, Object> entries = redis.opsForHash().entries(tokenKeyPrefix + TokenDigest.sha256Hex(token));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> hash = new HashMap<>();
        entries.forEach((field, value) -> hash.put((String) field, (String) value));
        return Optional.of(mapper.toModel(PendingEmailChangeRecord.fromHash(hash), token));
    }

    @Override
    public void discardForUser(UserId userId) {
        redis.execute(INVALIDATE, List.of(userKeyPrefix + userId.value()), tokenKeyPrefix);
    }
}
