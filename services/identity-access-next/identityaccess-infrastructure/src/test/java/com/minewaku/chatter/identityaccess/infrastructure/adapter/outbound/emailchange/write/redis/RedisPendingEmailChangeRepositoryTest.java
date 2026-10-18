package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.emailchange.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisPendingEmailChangeRepositoryTest extends RedisTestSupport {

    private static final String PREFIX = "test:";

    private StringRedisTemplate redis;
    private RedisPendingEmailChangeRepository repository;
    private Instant now;

    @BeforeEach
    void setUp() {
        redis = newTemplate();
        flushAll(redis);
        repository = new RedisPendingEmailChangeRepository(
                redis, new RedisKeyProperties(PREFIX), new PendingEmailChangeRecordMapper());
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private PendingEmailChange issue(long userId, String rawToken, Duration lifetime) {
        return PendingEmailChange.issue(
                rawToken,
                new UserId(userId),
                new Email("new" + userId + "@example.com"),
                Clock.fixed(now, ZoneOffset.UTC),
                lifetime);
    }

    private String tokenKey(String rawToken) {
        return PREFIX + "email-change:token:" + TokenDigest.sha256Hex(rawToken);
    }

    @Test
    void aSavedChangeIsFoundByThePresentedToken() {
        PendingEmailChange original = issue(1L, "token-one", Duration.ofHours(24));

        repository.save(original);

        assertThat(repository.findByPresentedToken("token-one"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void keysAndValuesUseTheEmailChangeFamilyAndNeverHoldTheRawToken() {
        repository.save(issue(2L, "super-secret-token", Duration.ofHours(24)));

        Set<String> keys = redis.keys(PREFIX + "*");
        assertThat(keys).containsExactlyInAnyOrder(tokenKey("super-secret-token"), PREFIX + "email-change:user:2");
        keys.forEach(key -> {
            assertThat(key).doesNotContain("super-secret-token");
            Object value = "hash".equals(redis.type(key).code())
                    ? redis.opsForHash().entries(key).values()
                    : redis.opsForValue().get(key);
            assertThat(String.valueOf(value)).doesNotContain("super-secret-token");
        });
    }

    @Test
    void keyExpiryEqualsTheTokenExpiry() {
        repository.save(issue(3L, "token-three", Duration.ofHours(24)));

        Long ttl = redis.getExpire(tokenKey("token-three"), TimeUnit.MILLISECONDS);

        assertThat(ttl)
                .isBetween(
                        Duration.ofHours(24).toMillis() - 5_000,
                        Duration.ofHours(24).toMillis());
    }

    @Test
    void discardRemovesAnOutstandingChangeAndItsPointer() {
        repository.save(issue(4L, "token-four", Duration.ofHours(1)));

        repository.discardForUser(new UserId(4L));

        assertThat(repository.findByPresentedToken("token-four")).isEmpty();
        assertThat(redis.hasKey(PREFIX + "email-change:user:4")).isFalse();
    }

    @Test
    void discardLeavesAConfirmedChangeReadableUntilItExpires() {
        PendingEmailChange change = issue(5L, "token-five", Duration.ofHours(1));
        repository.save(change);
        change.confirm(now.plusSeconds(1));
        repository.save(change);

        repository.discardForUser(new UserId(5L));

        assertThat(repository.findByPresentedToken("token-five"))
                .get()
                .satisfies(found -> assertThat(found.isConfirmed()).isTrue());
    }

    @Test
    void confirmingNeverExtendsTheExpiry() {
        PendingEmailChange change = issue(6L, "token-six", Duration.ofHours(1));
        repository.save(change);
        Long before = redis.getExpire(tokenKey("token-six"), TimeUnit.MILLISECONDS);

        change.confirm(now.plusSeconds(1));
        repository.save(change);

        assertThat(redis.getExpire(tokenKey("token-six"), TimeUnit.MILLISECONDS))
                .isLessThanOrEqualTo(before);
    }

    @Test
    void aReplacementForTheSameUserLeavesOnlyTheNewestPointer() {
        repository.save(issue(7L, "token-old", Duration.ofHours(1)));
        repository.discardForUser(new UserId(7L));
        repository.save(issue(7L, "token-new", Duration.ofHours(1)));

        assertThat(repository.findByPresentedToken("token-old")).isEmpty();
        assertThat(repository.findByPresentedToken("token-new")).isPresent();
        assertThat(redis.opsForValue().get(PREFIX + "email-change:user:7"))
                .isEqualTo(TokenDigest.sha256Hex("token-new"));
    }

    @Test
    void anExpiredChangeReadsAsUnknown() throws InterruptedException {
        repository.save(issue(8L, "token-eight", Duration.ofMillis(300)));
        assertThat(repository.findByPresentedToken("token-eight")).isPresent();

        Thread.sleep(500);

        assertThat(repository.findByPresentedToken("token-eight")).isEmpty();
    }

    @Test
    void blankOrNullTokenIsEmpty() {
        assertThat(repository.findByPresentedToken(null)).isEmpty();
        assertThat(repository.findByPresentedToken("  ")).isEmpty();
    }

    @Test
    void discardingForAUserWithoutAChangeDoesNothing() {
        repository.discardForUser(new UserId(999L));

        assertThat(redis.keys(PREFIX + "*")).isEmpty();
    }

    @Test
    void aRegistrationTokenIsNotAnEmailChangeTokenAndTheReverse() {
        // a registration token as the verification repository stores it, under its own key family
        redis.opsForHash()
                .putAll(
                        PREFIX + "verification:token:" + TokenDigest.sha256Hex("registration-token"),
                        java.util.Map.of(
                                "userId",
                                "9",
                                "email",
                                "reg9@example.com",
                                "createdAt",
                                Long.toString(now.getEpochSecond() * 1_000_000),
                                "expiresAt",
                                Long.toString(now.plusSeconds(3600).getEpochSecond() * 1_000_000)));
        repository.save(issue(10L, "change-token", Duration.ofHours(1)));

        assertThat(repository.findByPresentedToken("registration-token")).isEmpty();
        assertThat(redis.hasKey(PREFIX + "verification:token:" + TokenDigest.sha256Hex("change-token")))
                .isFalse();
        assertThat(redis.hasKey(PREFIX + "verification:user:10")).isFalse();
    }
}
