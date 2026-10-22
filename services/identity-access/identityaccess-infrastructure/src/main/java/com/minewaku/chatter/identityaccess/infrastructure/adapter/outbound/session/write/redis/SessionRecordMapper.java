package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import org.springframework.stereotype.Component;

@Component
class SessionRecordMapper {

    SessionRecord toRecord(AuthenticationSession session, String tokenDigest) {
        return new SessionRecord(
                session.sessionId(),
                session.userId().value(),
                session.deviceInfo(),
                RedisTime.toMicros(session.issuedAt()),
                RedisTime.toMicros(session.expiresAt()),
                session.lastUsedAt() == null ? null : RedisTime.toMicros(session.lastUsedAt()),
                session.revokedAt() == null ? null : RedisTime.toMicros(session.revokedAt()),
                tokenDigest);
    }

    AuthenticationSession toModel(SessionRecord record) {
        return AuthenticationSession.reconstitute(
                record.sessionId(),
                new UserId(record.userId()),
                record.deviceInfo(),
                RedisTime.fromMicros(record.issuedAt()),
                RedisTime.fromMicros(record.expiresAt()),
                record.lastUsedAt() == null ? null : RedisTime.fromMicros(record.lastUsedAt()),
                record.revokedAt() == null ? null : RedisTime.fromMicros(record.revokedAt()));
    }
}
