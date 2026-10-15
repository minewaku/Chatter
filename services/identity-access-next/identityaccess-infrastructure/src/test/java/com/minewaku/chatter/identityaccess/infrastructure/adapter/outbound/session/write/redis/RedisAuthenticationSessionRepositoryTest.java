package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.SessionProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisAuthenticationSessionRepositoryTest extends RedisTestSupport {

    private static final String PREFIX = "test:";
    private static final Duration RETENTION = Duration.ofDays(30);

    private StringRedisTemplate redis;
    private RedisAuthenticationSessionRepository repository;
    private Instant now;

    @BeforeEach
    void setUp() {
        redis = newTemplate();
        flushAll(redis);
        repository = new RedisAuthenticationSessionRepository(
                redis,
                new RedisKeyProperties(PREFIX),
                new SessionProperties(Duration.ofDays(30), RETENTION),
                new SessionRecordMapper());
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private AuthenticationSession session(String id, long userId) {
        return AuthenticationSession.issue(id, new UserId(userId), "device-" + id, now, Duration.ofHours(1));
    }

    @Test
    void createdSessionIsFoundByTokenAndById() {
        AuthenticationSession original = session("s1", 1L);

        repository.create(original, "raw-refresh-1");

        assertThat(repository.findByPresentedToken("raw-refresh-1"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(original);
        assertThat(repository.findBySessionId("s1"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void storedKeysAndValuesNeverContainTheRawToken() {
        repository.create(session("s2", 2L), "very-secret-refresh");

        for (String key : redis.keys(PREFIX + "*")) {
            assertThat(key).doesNotContain("very-secret-refresh");
            switch (String.valueOf(redis.type(key).code())) {
                case "hash" -> assertThat(redis.opsForHash().entries(key).toString())
                        .doesNotContain("very-secret-refresh");
                case "zset" -> assertThat(String.valueOf(redis.opsForZSet().range(key, 0, -1)))
                        .doesNotContain("very-secret-refresh");
                default -> assertThat(String.valueOf(redis.opsForValue().get(key)))
                        .doesNotContain("very-secret-refresh");
            }
        }
        assertThat(redis.opsForHash().get(PREFIX + "session:s2", "tokenDigest"))
                .isEqualTo(TokenDigest.sha256Hex("very-secret-refresh"));
    }

    @Test
    void unknownTokenOrBlankTokenIsEmpty() {
        assertThat(repository.findByPresentedToken("nope")).isEmpty();
        assertThat(repository.findByPresentedToken(" ")).isEmpty();
        assertThat(repository.findBySessionId("missing")).isEmpty();
    }

    @Test
    void touchSetsLastUsedForALiveSessionAndNeverMovesItBackwards() {
        repository.create(session("s3", 3L), "raw-3");

        assertThat(repository.touchIfUsable("s3", now.plusSeconds(10))).isTrue();
        assertThat(repository.touchIfUsable("s3", now.plusSeconds(5))).isTrue();

        assertThat(repository.findBySessionId("s3").orElseThrow().lastUsedAt()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    void touchIsFalseForUnknownRevokedAndExpiredSessions() {
        repository.create(session("s4", 4L), "raw-4");
        repository.create(session("s5", 4L), "raw-5");
        repository.revokeBySessionId(new UserId(4L), "s5", now.plusSeconds(1));

        assertThat(repository.touchIfUsable("missing", now)).isFalse();
        assertThat(repository.touchIfUsable("s5", now.plusSeconds(2))).isFalse();
        // expired exactly when now reaches expiresAt
        assertThat(repository.touchIfUsable("s4", now.plus(Duration.ofHours(1)).minusNanos(1_000)))
                .isTrue();
        assertThat(repository.touchIfUsable("s4", now.plus(Duration.ofHours(1))))
                .isFalse();
        assertThat(repository.touchIfUsable("s4", now.plus(Duration.ofHours(2))))
                .isFalse();
    }

    @Test
    void revokeBySessionIdWorksOnceAndOnlyForTheOwner() {
        repository.create(session("s6", 6L), "raw-6");

        assertThat(repository.revokeBySessionId(new UserId(999L), "s6", now)).isFalse();
        assertThat(repository.revokeBySessionId(new UserId(6L), "s6", now.plusSeconds(1)))
                .isTrue();
        assertThat(repository.revokeBySessionId(new UserId(6L), "s6", now.plusSeconds(2)))
                .isFalse();
        assertThat(repository.findBySessionId("s6").orElseThrow().revokedAt()).isEqualTo(now.plusSeconds(1));
    }

    @Test
    void revokeOtherSessionsLeavesTheCurrentOneAndOtherUsersAlone() {
        repository.create(session("a", 7L), "raw-a");
        repository.create(session("b", 7L), "raw-b");
        repository.create(session("c", 7L), "raw-c");
        repository.create(session("other", 8L), "raw-o");

        assertThat(repository.revokeOtherSessions(new UserId(7L), "a", now.plusSeconds(1)))
                .isEqualTo(2);

        assertThat(repository.findBySessionId("a").orElseThrow().revokedAt()).isNull();
        assertThat(repository.findBySessionId("b").orElseThrow().revokedAt()).isNotNull();
        assertThat(repository.findBySessionId("c").orElseThrow().revokedAt()).isNotNull();
        assertThat(repository.findBySessionId("other").orElseThrow().revokedAt())
                .isNull();
        assertThat(repository.revokeOtherSessions(new UserId(7L), "a", now.plusSeconds(2)))
                .isZero();
    }

    @Test
    void revokeAllRevokesEveryRetainedSessionOfTheUser() {
        repository.create(session("x", 9L), "raw-x");
        repository.create(session("y", 9L), "raw-y");

        assertThat(repository.revokeAllByUserId(new UserId(9L), now.plusSeconds(1)))
                .isEqualTo(2);
        assertThat(repository.revokeAllByUserId(new UserId(9L), now.plusSeconds(2)))
                .isZero();
    }

    @Test
    void expiredAndRevokedSessionsStayReadableUntilTheRetentionEnds() {
        AuthenticationSession expired = AuthenticationSession.reconstitute(
                "old",
                new UserId(10L),
                "d",
                now.minus(Duration.ofHours(2)),
                now.minus(Duration.ofHours(1)),
                null,
                null);
        repository.create(expired, "raw-old");
        repository.create(session("live", 10L), "raw-live");
        repository.revokeBySessionId(new UserId(10L), "live", now.plusSeconds(1));

        assertThat(repository.findByPresentedToken("raw-old")).isPresent();
        assertThat(repository.findByPresentedToken("raw-live")).isPresent();

        long expectedMillis = Duration.between(now, expired.expiresAt()).toMillis() + RETENTION.toMillis();
        Long ttl = redis.getExpire(PREFIX + "session:old", TimeUnit.MILLISECONDS);
        assertThat(ttl).isBetween(expectedMillis - 5_000, expectedMillis + 1_000);
    }

    @Test
    void onlyOneOfManyConcurrentRevokesSucceeds() throws Exception {
        repository.create(session("race", 11L), "raw-race");
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Boolean> task = () -> {
                    start.await();
                    return repository.revokeBySessionId(new UserId(11L), "race", now.plusSeconds(1));
                };
                futures.add(pool.submit(task));
            }
            start.countDown();
            int successes = 0;
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lostEntriesReadAsAbsent() {
        repository.create(session("gone", 12L), "raw-gone");

        flushAll(redis);

        assertThat(repository.findByPresentedToken("raw-gone")).isEmpty();
        assertThat(repository.touchIfUsable("gone", now)).isFalse();
        assertThat(repository.revokeBySessionId(new UserId(12L), "gone", now)).isFalse();
    }
}
