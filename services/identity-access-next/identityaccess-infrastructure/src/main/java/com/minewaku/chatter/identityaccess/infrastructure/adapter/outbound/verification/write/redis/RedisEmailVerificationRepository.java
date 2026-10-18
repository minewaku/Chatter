package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.write.redis;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisScripts;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * Source of truth for proofs of mailbox ownership, for every purpose. Each purpose has its own key family, so a token
 * issued for one purpose is never found through another. Only the SHA-256 digest of a token is used as a key; the
 * entry expires on its own at the token's expiry, so no cleanup job is needed.
 */
@Repository
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

    private static final DefaultRedisScript<Long> SAVE = RedisScripts.load("verification-save.lua", Long.class);
    private static final DefaultRedisScript<Long> INVALIDATE =
            RedisScripts.load("verification-invalidate-for-user.lua", Long.class);

    private final StringRedisTemplate redis;
    private final VerificationRecordMapper mapper;
    private final String keyPrefix;

    RedisEmailVerificationRepository(
            StringRedisTemplate redis, RedisKeyProperties keys, VerificationRecordMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
        this.keyPrefix = keys.keyPrefix();
    }

    /** No default branch: a new purpose does not compile until its key family is stated. */
    private String family(VerificationPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> keyPrefix + "verification:";
            case EMAIL_CHANGE -> keyPrefix + "email-change:";
        };
    }

    private String tokenKeyPrefix(VerificationPurpose purpose) {
        return family(purpose) + "token:";
    }

    private String userKeyPrefix(VerificationPurpose purpose) {
        return family(purpose) + "user:";
    }

    @Override
    public void save(EmailVerification verification) {
        VerificationRecord record = mapper.toRecord(verification);
        VerificationPurpose purpose = verification.purpose();
        String digest = TokenDigest.sha256Hex(verification.rawToken());

        List<Object> args = new ArrayList<>();
        args.add(Long.toString(RedisTime.toMillisFloor(record.expiresAt())));
        args.add(digest);
        args.add(record.verifiedAt() == null ? "1" : "0");
        record.toHash().forEach((field, value) -> {
            args.add(field);
            args.add(value);
        });

        redis.execute(
                SAVE,
                List.of(tokenKeyPrefix(purpose) + digest, userKeyPrefix(purpose) + record.userId()),
                args.toArray());
    }

    @Override
    public Optional<EmailVerification> findByPresentedToken(VerificationPurpose purpose, String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Map<Object, Object> entries =
                redis.opsForHash().entries(tokenKeyPrefix(purpose) + TokenDigest.sha256Hex(token));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> hash = new HashMap<>();
        entries.forEach((field, value) -> hash.put((String) field, (String) value));
        return Optional.of(mapper.toModel(VerificationRecord.fromHash(hash), purpose, token));
    }

    @Override
    public void invalidateForUser(VerificationPurpose purpose, UserId userId) {
        redis.execute(INVALIDATE, List.of(userKeyPrefix(purpose) + userId.value()), tokenKeyPrefix(purpose));
    }
}
