package com.minewaku.chatter.identityaccess.authentication.api.query;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;
import java.time.Instant;
import java.util.List;

public interface FindSessionsQuery
        extends UseCaseHandler<FindSessionsQuery.Command, List<FindSessionsQuery.SessionView>> {

    record Command(long userId) {}

    record SessionView(
            String sessionId,
            long userId,
            String deviceInfo,
            Instant issuedAt,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant revokedAt) {}
}
