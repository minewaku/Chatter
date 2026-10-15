package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.read.redis;

import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.authentication.internal.port.query.AuthenticationSessionViewRepository;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisScripts;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisTime;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * Read side of the session store. It reads the same hashes the write side maintains, in one round trip, and maps
 * them straight to the query result type. It shares no code with the write side.
 */
@Repository
public class RedisAuthenticationSessionViewRepository implements AuthenticationSessionViewRepository {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final DefaultRedisScript<List> LIST_BY_USER =
            (DefaultRedisScript) RedisScripts.load("session-list-by-user.lua", List.class);

    private final StringRedisTemplate redis;
    private final String sessionKeyPrefix;
    private final String userKeyPrefix;

    RedisAuthenticationSessionViewRepository(StringRedisTemplate redis, RedisKeyProperties keys) {
        this.redis = redis;
        this.sessionKeyPrefix = keys.keyPrefix() + "session:";
        this.userKeyPrefix = keys.keyPrefix() + "session:user:";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FindSessionsQuery.SessionView> findByUserId(UserId userId) {
        List<List<Object>> rows =
                redis.execute(LIST_BY_USER, List.of(userKeyPrefix + userId.value()), sessionKeyPrefix);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<FindSessionsQuery.SessionView> views = new ArrayList<>(rows.size());
        for (List<Object> flat : rows) {
            views.add(toView(flat));
        }
        views.sort(Comparator.comparing(FindSessionsQuery.SessionView::issuedAt)
                .reversed()
                .thenComparing(FindSessionsQuery.SessionView::sessionId));
        return views;
    }

    private static FindSessionsQuery.SessionView toView(List<Object> flat) {
        Map<String, String> hash = new HashMap<>();
        for (int i = 0; i + 1 < flat.size(); i += 2) {
            hash.put(String.valueOf(flat.get(i)), String.valueOf(flat.get(i + 1)));
        }
        return new FindSessionsQuery.SessionView(
                hash.get("sessionId"),
                Long.parseLong(hash.get("userId")),
                hash.get("deviceInfo"),
                RedisTime.fromMicros(Long.parseLong(hash.get("issuedAt"))),
                RedisTime.fromMicros(Long.parseLong(hash.get("expiresAt"))),
                optionalInstant(hash.get("lastUsedAt")),
                optionalInstant(hash.get("revokedAt")));
    }

    private static java.time.Instant optionalInstant(String micros) {
        return micros == null ? null : RedisTime.fromMicros(Long.parseLong(micros));
    }
}
