package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.write.redis;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import org.springframework.stereotype.Component;

@Component
class VerificationRecordMapper {

    VerificationRecord toRecord(EmailVerification verification) {
        return new VerificationRecord(
                verification.userId().value(),
                verification.email().value(),
                RedisTime.toMicros(verification.createdAt()),
                RedisTime.toMicros(verification.expiresAt()),
                verification.verifiedAt() == null ? null : RedisTime.toMicros(verification.verifiedAt()));
    }

    /** The raw token comes from the value the caller presented; it is never read back from Redis. */
    EmailVerification toModel(VerificationRecord record, String presentedToken) {
        return EmailVerification.reconstitute(
                presentedToken,
                new UserId(record.userId()),
                new Email(record.email()),
                RedisTime.fromMicros(record.createdAt()),
                RedisTime.fromMicros(record.expiresAt()),
                record.verifiedAt() == null ? null : RedisTime.fromMicros(record.verifiedAt()));
    }
}
