package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.read.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis.RedisSessionWriterFactory;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisAuthenticationSessionViewRepositoryTest extends RedisTestSupport {

    private static final String PREFIX = "test:";

    private StringRedisTemplate redis;
    private AuthenticationSessionRepository writer;
    private RedisAuthenticationSessionViewRepository view;
    private Instant now;

    @BeforeEach
    void setUp() {
        redis = newTemplate();
        flushAll(redis);
        writer = RedisSessionWriterFactory.create(redis, PREFIX);
        view = new RedisAuthenticationSessionViewRepository(redis, new RedisKeyProperties(PREFIX));
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private AuthenticationSession session(String id, long userId, Instant issuedAt) {
        return AuthenticationSession.issue(id, new UserId(userId), "device-" + id, issuedAt, Duration.ofHours(1));
    }

    @Test
    void viewCarriesTheStoredFields() {
        AuthenticationSession original = session("v1", 1L, now);
        writer.create(original, "raw-v1");
        writer.touchIfUsable("v1", now.plusSeconds(3));
        writer.create(session("v2", 1L, now.minusSeconds(60)), "raw-v2");
        writer.revokeBySessionId(new UserId(1L), "v2", now.plusSeconds(4));

        List<FindSessionsQuery.SessionView> views = view.findByUserId(new UserId(1L));

        assertThat(views).hasSize(2);
        FindSessionsQuery.SessionView first = views.get(0);
        assertThat(first.sessionId()).isEqualTo("v1");
        assertThat(first.userId()).isEqualTo(1L);
        assertThat(first.deviceInfo()).isEqualTo("device-v1");
        assertThat(first.issuedAt()).isEqualTo(original.issuedAt());
        assertThat(first.expiresAt()).isEqualTo(original.expiresAt());
        assertThat(first.lastUsedAt()).isEqualTo(now.plusSeconds(3));
        assertThat(first.revokedAt()).isNull();
        assertThat(views.get(1).revokedAt()).isEqualTo(now.plusSeconds(4));
    }

    @Test
    void sortedByIssuedAtDescendingThenSessionIdAscending() {
        writer.create(session("b", 2L, now), "raw-b");
        writer.create(session("a", 2L, now), "raw-a");
        writer.create(session("c", 2L, now.plusSeconds(10)), "raw-c");
        writer.create(session("z", 2L, now.minusSeconds(10)), "raw-z");

        assertThat(view.findByUserId(new UserId(2L)))
                .extracting(FindSessionsQuery.SessionView::sessionId)
                .containsExactly("c", "a", "b", "z");
    }

    @Test
    void sessionsWhoseHashExpiredAreOmittedAndPrunedFromTheIndex() {
        writer.create(session("keep", 3L, now), "raw-keep");
        writer.create(session("lost", 3L, now.plusSeconds(1)), "raw-lost");
        redis.delete(PREFIX + "session:lost");

        assertThat(view.findByUserId(new UserId(3L)))
                .extracting(FindSessionsQuery.SessionView::sessionId)
                .containsExactly("keep");
        assertThat(redis.opsForZSet().range(PREFIX + "session:user:3", 0, -1)).containsExactly("keep");
    }

    @Test
    void unknownUserHasNoSessions() {
        assertThat(view.findByUserId(new UserId(404L))).isEmpty();
    }

    @Test
    void hundredRetainedSessionsAreReadInASingleScriptCall() {
        for (int i = 0; i < 100; i++) {
            writer.create(session(String.format("s%03d", i), 4L, now.plusMillis(i)), "raw-" + i);
        }
        redis.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().resetConfigStats();
            return null;
        });

        List<FindSessionsQuery.SessionView> views = view.findByUserId(new UserId(4L));

        assertThat(views).hasSize(100);
        Properties stats = redis.execute((RedisCallback<Properties>)
                connection -> connection.serverCommands().info("commandstats"));
        long scriptCalls = callsOf(stats, "cmdstat_evalsha") + callsOf(stats, "cmdstat_eval");
        assertThat(scriptCalls).isBetween(1L, 2L);
        assertThat(callsOf(stats, "cmdstat_hgetall")).isEqualTo(100);
    }

    private static long callsOf(Properties stats, String command) {
        String line = stats.getProperty(command);
        if (line == null) {
            return 0;
        }
        for (String part : line.split(",")) {
            if (part.startsWith("calls=")) {
                return Long.parseLong(part.substring("calls=".length()));
            }
        }
        return 0;
    }
}
