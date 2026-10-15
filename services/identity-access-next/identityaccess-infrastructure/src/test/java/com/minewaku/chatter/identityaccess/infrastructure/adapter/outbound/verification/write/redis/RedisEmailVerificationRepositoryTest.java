package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisEmailVerificationRepositoryTest extends RedisTestSupport {

    private static final String PREFIX = "test:";

    private StringRedisTemplate redis;
    private RedisEmailVerificationRepository repository;
    private Instant now;

    @BeforeEach
    void setUp() {
        redis = newTemplate();
        flushAll(redis);
        repository = new RedisEmailVerificationRepository(
                redis, new RedisKeyProperties(PREFIX), new VerificationRecordMapper());
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private EmailVerification issue(long userId, String rawToken, Duration lifetime) {
        return EmailVerification.issue(
                rawToken,
                new UserId(userId),
                new Email("user" + userId + "@example.com"),
                Clock.fixed(now, ZoneOffset.UTC),
                lifetime);
    }

    @Test
    void savedTokenIsFoundByThePresentedValue() {
        EmailVerification original = issue(1L, "token-one", Duration.ofHours(24));

        repository.save(original);

        assertThat(repository.findByPresentedToken("token-one"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void keysAndValuesContainOnlyTheDigestNeverTheRawToken() {
        repository.save(issue(2L, "super-secret-token", Duration.ofHours(24)));

        Set<String> keys = redis.keys(PREFIX + "*");
        assertThat(keys)
                .containsExactlyInAnyOrder(
                        PREFIX + "verification:token:" + TokenDigest.sha256Hex("super-secret-token"),
                        PREFIX + "verification:user:2");
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

        Long ttl = redis.getExpire(
                PREFIX + "verification:token:" + TokenDigest.sha256Hex("token-three"),
                java.util.concurrent.TimeUnit.MILLISECONDS);

        assertThat(ttl)
                .isBetween(
                        Duration.ofHours(24).toMillis() - 5_000,
                        Duration.ofHours(24).toMillis());
    }

    @Test
    void savingAfterConsumptionNeverExtendsTheExpiry() {
        EmailVerification verification = issue(4L, "token-four", Duration.ofHours(1));
        repository.save(verification);
        String tokenKey = PREFIX + "verification:token:" + TokenDigest.sha256Hex("token-four");
        Long before = redis.getExpire(tokenKey, java.util.concurrent.TimeUnit.MILLISECONDS);

        verification.verify(now.plusSeconds(1));
        repository.save(verification);

        Long after = redis.getExpire(tokenKey, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertThat(after).isLessThanOrEqualTo(before);
        assertThat(repository.findByPresentedToken("token-four"))
                .get()
                .satisfies(found -> assertThat(found.isVerified()).isTrue());
    }

    @Test
    void anExpiredTokenReadsAsUnknown() throws InterruptedException {
        repository.save(issue(5L, "token-five", Duration.ofMillis(300)));
        assertThat(repository.findByPresentedToken("token-five")).isPresent();

        Thread.sleep(500);

        assertThat(repository.findByPresentedToken("token-five")).isEmpty();
    }

    @Test
    void blankOrNullTokenIsEmpty() {
        assertThat(repository.findByPresentedToken(null)).isEmpty();
        assertThat(repository.findByPresentedToken("  ")).isEmpty();
    }

    @Test
    void invalidateRemovesAnOutstandingTokenAndItsPointer() {
        repository.save(issue(6L, "token-six", Duration.ofHours(1)));

        repository.invalidateForUser(new UserId(6L));

        assertThat(repository.findByPresentedToken("token-six")).isEmpty();
        assertThat(redis.hasKey(PREFIX + "verification:user:6")).isFalse();
    }

    @Test
    void invalidateLeavesAConsumedTokenReadableUntilItExpires() {
        EmailVerification verification = issue(7L, "token-seven", Duration.ofHours(1));
        repository.save(verification);
        verification.verify(now.plusSeconds(1));
        repository.save(verification);

        repository.invalidateForUser(new UserId(7L));

        assertThat(repository.findByPresentedToken("token-seven")).isPresent();
    }

    @Test
    void invalidateForAUserWithoutATokenDoesNothing() {
        repository.invalidateForUser(new UserId(999L));

        assertThat(redis.keys(PREFIX + "*")).isEmpty();
    }

    @Test
    void entriesLostEarlyReadAsUnknown() {
        repository.save(issue(8L, "token-eight", Duration.ofHours(1)));

        flushAll(redis);

        assertThat(repository.findByPresentedToken("token-eight")).isEmpty();
    }
}
