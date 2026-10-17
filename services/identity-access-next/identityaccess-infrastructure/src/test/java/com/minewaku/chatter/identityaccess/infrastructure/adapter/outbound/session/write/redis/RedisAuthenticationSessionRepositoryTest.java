package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.SessionProperties;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
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
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);

    private StringRedisTemplate redis;
    private RedisAuthenticationSessionRepository repository;
    private Instant now;

    @BeforeEach
    void setUp() {
        redis = newTemplate();
        flushAll(redis);
        repository = repositoryWithAccessTtl(ACCESS_TTL);
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private RedisAuthenticationSessionRepository repositoryWithAccessTtl(Duration accessTokenTtl) {
        return new RedisAuthenticationSessionRepository(
                redis,
                new RedisKeyProperties(PREFIX),
                new SessionProperties(Duration.ofDays(30), RETENTION),
                new JwtProperties(null, null, null, null, accessTokenTtl),
                new SessionRecordMapper());
    }

    private String revocationKey(long userId, String sessionId) {
        return PREFIX + "access-revoked:" + userId + ":" + sessionId;
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
    void rotationReplacesTheCredentialAndGivesTheSessionANewExpiry() {
        repository.create(session("r1", 3L), "raw-old");
        Instant at = now.plusSeconds(600);
        Instant newExpiry = at.plus(Duration.ofDays(30));

        boolean rotated = repository.rotate("r1", "raw-old", "raw-new", at, newExpiry);

        assertThat(rotated).isTrue();
        assertThat(repository.findByPresentedToken("raw-old")).isEmpty();
        AuthenticationSession renewed =
                repository.findByPresentedToken("raw-new").orElseThrow();
        assertThat(renewed.sessionId()).isEqualTo("r1");
        assertThat(renewed.expiresAt()).isEqualTo(newExpiry);
        assertThat(renewed.lastUsedAt()).isEqualTo(at);
        assertThat(renewed.issuedAt()).isEqualTo(now);
        assertThat(redis.opsForHash().get(PREFIX + "session:r1", "tokenDigest"))
                .isEqualTo(TokenDigest.sha256Hex("raw-new"));
    }

    @Test
    void rotationRecomputesKeyLifetimesFromTheNewExpiry() {
        repository.create(session("r2", 3L), "raw-a");
        Instant at = now.plusSeconds(60);
        Instant newExpiry = at.plus(Duration.ofDays(30));

        repository.rotate("r2", "raw-a", "raw-b", at, newExpiry);

        long expectedMillis = Duration.between(Instant.now(), newExpiry).toMillis() + RETENTION.toMillis();
        for (String key : List.of(PREFIX + "session:r2", PREFIX + "session:token:" + TokenDigest.sha256Hex("raw-b"))) {
            assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS))
                    .as(key)
                    .isBetween(expectedMillis - 10_000, expectedMillis + 1_000);
        }
        assertThat(redis.hasKey(PREFIX + "session:token:" + TokenDigest.sha256Hex("raw-a")))
                .isFalse();
    }

    @Test
    void rotationNeverMovesLastUseBackwards() {
        repository.create(session("r3", 3L), "raw-1");
        repository.rotate("r3", "raw-1", "raw-2", now.plusSeconds(100), now.plusSeconds(4_000));

        repository.rotate("r3", "raw-2", "raw-3", now.plusSeconds(50), now.plusSeconds(4_000));

        assertThat(repository.findBySessionId("r3").orElseThrow().lastUsedAt()).isEqualTo(now.plusSeconds(100));
    }

    @Test
    void rotationIsFalseAndChangesNothingForUnknownRevokedExpiredAndForeignCredentials() {
        repository.create(session("r4", 4L), "raw-r4");
        repository.create(session("r5", 4L), "raw-r5");
        repository.revokeBySessionId(new UserId(4L), "r5", now.plusSeconds(1));
        Instant later = now.plusSeconds(30);

        assertThat(repository.rotate("missing", "raw-r4", "n1", later, later.plusSeconds(60)))
                .isFalse();
        assertThat(repository.rotate("r5", "raw-r5", "n2", now.plusSeconds(2), later.plusSeconds(60)))
                .isFalse();
        // the credential belongs to another session
        assertThat(repository.rotate("r4", "raw-r5", "n3", later, later.plusSeconds(60)))
                .isFalse();
        // unknown credential
        assertThat(repository.rotate("r4", "nope", "n4", later, later.plusSeconds(60)))
                .isFalse();
        // expired exactly when now reaches expiresAt
        assertThat(repository.rotate("r4", "raw-r4", "n5", now.plus(Duration.ofHours(1)), now.plus(Duration.ofDays(2))))
                .isFalse();

        assertThat(repository.findByPresentedToken("raw-r4").orElseThrow().lastUsedAt())
                .isNull();
        assertThat(repository.findByPresentedToken("raw-r5")).isPresent();
        assertThat(repository.findByPresentedToken("n1")).isEmpty();
    }

    @Test
    void onlyOneOfManyConcurrentRotationsWithTheSameCredentialSucceeds() throws Exception {
        repository.create(session("r6", 5L), "raw-shared");
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String next = "raw-next-" + i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return repository.rotate("r6", "raw-shared", next, now.plusSeconds(5), now.plusSeconds(4_000));
                }));
            }
            start.countDown();
            int successes = 0;
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(repository.findByPresentedToken("raw-shared")).isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void revokingOneSessionCreatesItsRevocationEntryWithTheAccessTokenLifetime() {
        repository.create(session("v1", 20L), "raw-v1");
        repository.create(session("v2", 20L), "raw-v2");

        assertThat(repository.revokeBySessionId(new UserId(20L), "v1", now.plusSeconds(1)))
                .isTrue();

        assertThat(redis.hasKey(revocationKey(20L, "v1"))).isTrue();
        assertThat(redis.hasKey(revocationKey(20L, "v2"))).isFalse();
        assertThat(redis.getExpire(revocationKey(20L, "v1"), TimeUnit.MILLISECONDS))
                .isBetween(1L, ACCESS_TTL.toMillis());
    }

    @Test
    void aRepeatedOrRefusedRevokeCreatesNoEntryAndDoesNotResetTheLifetime() throws Exception {
        repository.create(session("v3", 21L), "raw-v3");
        repository.revokeBySessionId(new UserId(21L), "v3", now.plusSeconds(1));
        Long first = redis.getExpire(revocationKey(21L, "v3"), TimeUnit.MILLISECONDS);
        Thread.sleep(50);

        assertThat(repository.revokeBySessionId(new UserId(21L), "v3", now.plusSeconds(2)))
                .isFalse();
        assertThat(repository.revokeBySessionId(new UserId(999L), "v3", now.plusSeconds(2)))
                .isFalse();

        assertThat(redis.getExpire(revocationKey(21L, "v3"), TimeUnit.MILLISECONDS))
                .isLessThan(first);
        assertThat(redis.keys(PREFIX + "access-revoked:*")).hasSize(1);
    }

    @Test
    void revokingOthersCreatesAnEntryPerRevokedSessionAndSparesTheKeptOne() {
        repository.create(session("k1", 22L), "raw-k1");
        repository.create(session("k2", 22L), "raw-k2");
        repository.create(session("k3", 22L), "raw-k3");

        repository.revokeOtherSessions(new UserId(22L), "k1", now.plusSeconds(1));

        assertThat(redis.hasKey(revocationKey(22L, "k1"))).isFalse();
        assertThat(redis.hasKey(revocationKey(22L, "k2"))).isTrue();
        assertThat(redis.hasKey(revocationKey(22L, "k3"))).isTrue();
    }

    @Test
    void revokingAllCreatesAnEntryForEveryRevokedSessionAndNoneForAlreadyRevokedOnes() {
        repository.create(session("m1", 23L), "raw-m1");
        repository.create(session("m2", 23L), "raw-m2");
        repository.create(session("m3", 23L), "raw-m3");
        repository.revokeBySessionId(new UserId(23L), "m3", now.plusSeconds(1));
        redis.delete(revocationKey(23L, "m3"));

        int revoked = repository.revokeAllByUserId(new UserId(23L), now.plusSeconds(2));

        assertThat(revoked).isEqualTo(2);
        assertThat(redis.hasKey(revocationKey(23L, "m1"))).isTrue();
        assertThat(redis.hasKey(revocationKey(23L, "m2"))).isTrue();
        assertThat(redis.hasKey(revocationKey(23L, "m3"))).isFalse();
    }

    @Test
    void revocationEntriesDisappearOnTheirOwnWhileTheSessionStaysRevoked() throws Exception {
        RedisAuthenticationSessionRepository shortLived = repositoryWithAccessTtl(Duration.ofSeconds(1));
        shortLived.create(session("t1", 24L), "raw-t1");

        shortLived.revokeBySessionId(new UserId(24L), "t1", now.plusSeconds(1));
        assertThat(redis.hasKey(revocationKey(24L, "t1"))).isTrue();

        long deadline = System.currentTimeMillis() + 5_000;
        while (redis.hasKey(revocationKey(24L, "t1")) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertThat(redis.hasKey(revocationKey(24L, "t1"))).isFalse();
        assertThat(shortLived.findBySessionId("t1").orElseThrow().revokedAt()).isNotNull();
        assertThat(shortLived.rotate("t1", "raw-t1", "raw-t1b", now.plusSeconds(5), now.plusSeconds(500)))
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
        assertThat(repository.rotate("gone", "raw-gone", "raw-new", now, now.plusSeconds(60)))
                .isFalse();
        assertThat(repository.revokeBySessionId(new UserId(12L), "gone", now)).isFalse();
    }
}
