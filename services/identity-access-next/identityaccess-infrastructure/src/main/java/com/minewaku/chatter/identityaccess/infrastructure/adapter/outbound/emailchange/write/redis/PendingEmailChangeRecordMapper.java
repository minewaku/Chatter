package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.emailchange.write.redis;

import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import org.springframework.stereotype.Component;

@Component
class PendingEmailChangeRecordMapper {

    PendingEmailChangeRecord toRecord(PendingEmailChange change) {
        return new PendingEmailChangeRecord(
                change.userId().value(),
                change.newEmail().value(),
                RedisTime.toMicros(change.createdAt()),
                RedisTime.toMicros(change.expiresAt()),
                change.confirmedAt() == null ? null : RedisTime.toMicros(change.confirmedAt()));
    }

    /** The raw token comes from the value the caller presented; it is never read back from Redis. */
    PendingEmailChange toModel(PendingEmailChangeRecord record, String presentedToken) {
        return PendingEmailChange.reconstitute(
                presentedToken,
                new UserId(record.userId()),
                new Email(record.email()),
                RedisTime.fromMicros(record.createdAt()),
                RedisTime.fromMicros(record.expiresAt()),
                record.confirmedAt() == null ? null : RedisTime.fromMicros(record.confirmedAt()));
    }
}
