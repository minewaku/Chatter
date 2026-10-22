package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    private EmailVerification issue(VerificationPurpose purpose, long userId, String rawToken, Duration lifetime) {
        return EmailVerification.issue(
                purpose,
                rawToken,
                new UserId(userId),
                new Email("user" + userId + "@example.com"),
                Clock.fixed(now, ZoneOffset.UTC),
                lifetime);
    }

    /** The key family each purpose has always used; changing it would orphan links already sent. */
    private static String family(VerificationPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> PREFIX + "verification:";
            case EMAIL_CHANGE -> PREFIX + "email-change:";
        };
    }

    private static String tokenKey(VerificationPurpose purpose, String rawToken) {
        return family(purpose) + "token:" + TokenDigest.sha256Hex(rawToken);
    }

    private static String userKey(VerificationPurpose purpose, long userId) {
        return family(purpose) + "user:" + userId;
    }

    private static long micros(Instant instant) {
        return instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1_000;
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void savedTokenIsFoundByThePresentedValue(VerificationPurpose purpose) {
        EmailVerification original = issue(purpose, 1L, "token-one", Duration.ofHours(24));

        repository.save(original);

        assertThat(repository.findByPresentedToken(purpose, "token-one"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void keysAndValuesContainOnlyTheDigestNeverTheRawToken(VerificationPurpose purpose) {
        repository.save(issue(purpose, 2L, "super-secret-token", Duration.ofHours(24)));

        Set<String> keys = redis.keys(PREFIX + "*");
        assertThat(keys).containsExactlyInAnyOrder(tokenKey(purpose, "super-secret-token"), userKey(purpose, 2));
        keys.forEach(key -> {
            assertThat(key).doesNotContain("super-secret-token");
            Object value = "hash".equals(redis.type(key).code())
                    ? redis.opsForHash().entries(key).values()
                    : redis.opsForValue().get(key);
            assertThat(String.valueOf(value)).doesNotContain("super-secret-token");
        });
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void keyExpiryEqualsTheTokenExpiry(VerificationPurpose purpose) {
        repository.save(issue(purpose, 3L, "token-three", Duration.ofHours(24)));

        Long ttl = redis.getExpire(tokenKey(purpose, "token-three"), TimeUnit.MILLISECONDS);

        assertThat(ttl)
                .isBetween(
                        Duration.ofHours(24).toMillis() - 5_000,
                        Duration.ofHours(24).toMillis());
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void savingAfterConsumptionNeverExtendsTheExpiry(VerificationPurpose purpose) {
        EmailVerification verification = issue(purpose, 4L, "token-four", Duration.ofHours(1));
        repository.save(verification);
        String tokenKey = tokenKey(purpose, "token-four");
        Long before = redis.getExpire(tokenKey, TimeUnit.MILLISECONDS);

        verification.verify(now.plusSeconds(1));
        repository.save(verification);

        Long after = redis.getExpire(tokenKey, TimeUnit.MILLISECONDS);
        assertThat(after).isLessThanOrEqualTo(before);
        assertThat(repository.findByPresentedToken(purpose, "token-four"))
                .get()
                .satisfies(found -> assertThat(found.isVerified()).isTrue());
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void anExpiredTokenReadsAsUnknown(VerificationPurpose purpose) throws InterruptedException {
        repository.save(issue(purpose, 5L, "token-five", Duration.ofMillis(300)));
        assertThat(repository.findByPresentedToken(purpose, "token-five")).isPresent();

        Thread.sleep(500);

        assertThat(repository.findByPresentedToken(purpose, "token-five")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void blankOrNullTokenIsEmpty(VerificationPurpose purpose) {
        assertThat(repository.findByPresentedToken(purpose, null)).isEmpty();
        assertThat(repository.findByPresentedToken(purpose, "  ")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void invalidateRemovesAnOutstandingTokenAndItsPointer(VerificationPurpose purpose) {
        repository.save(issue(purpose, 6L, "token-six", Duration.ofHours(1)));

        repository.invalidateForUser(purpose, new UserId(6L));

        assertThat(repository.findByPresentedToken(purpose, "token-six")).isEmpty();
        assertThat(redis.hasKey(userKey(purpose, 6))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void invalidateLeavesAConsumedTokenReadableUntilItExpires(VerificationPurpose purpose) {
        EmailVerification verification = issue(purpose, 7L, "token-seven", Duration.ofHours(1));
        repository.save(verification);
        verification.verify(now.plusSeconds(1));
        repository.save(verification);

        repository.invalidateForUser(purpose, new UserId(7L));

        assertThat(repository.findByPresentedToken(purpose, "token-seven")).isPresent();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void invalidateForAUserWithoutATokenDoesNothing(VerificationPurpose purpose) {
        repository.invalidateForUser(purpose, new UserId(999L));

        assertThat(redis.keys(PREFIX + "*")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void entriesLostEarlyReadAsUnknown(VerificationPurpose purpose) {
        repository.save(issue(purpose, 8L, "token-eight", Duration.ofHours(1)));

        flushAll(redis);

        assertThat(repository.findByPresentedToken(purpose, "token-eight")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void aReplacementForTheSameUserLeavesOnlyTheNewestPointer(VerificationPurpose purpose) {
        repository.save(issue(purpose, 9L, "token-old", Duration.ofHours(1)));
        repository.invalidateForUser(purpose, new UserId(9L));
        repository.save(issue(purpose, 9L, "token-new", Duration.ofHours(1)));

        assertThat(repository.findByPresentedToken(purpose, "token-old")).isEmpty();
        assertThat(repository.findByPresentedToken(purpose, "token-new")).isPresent();
        assertThat(redis.opsForValue().get(userKey(purpose, 9))).isEqualTo(TokenDigest.sha256Hex("token-new"));
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void aTokenIsNotFoundUnderTheOtherPurpose(VerificationPurpose purpose) {
        VerificationPurpose other = other(purpose);
        repository.save(issue(purpose, 10L, "shared-token", Duration.ofHours(1)));

        assertThat(repository.findByPresentedToken(other, "shared-token")).isEmpty();
        assertThat(redis.hasKey(tokenKey(other, "shared-token"))).isFalse();
        assertThat(redis.hasKey(userKey(other, 10))).isFalse();
    }

    @Test
    void theSameRawTokenUnderBothPurposesGivesTwoIndependentProofs() {
        repository.save(issue(VerificationPurpose.REGISTRATION, 11L, "same-token", Duration.ofHours(1)));
        repository.save(issue(VerificationPurpose.EMAIL_CHANGE, 11L, "same-token", Duration.ofHours(1)));

        EmailVerification registration = repository
                .findByPresentedToken(VerificationPurpose.REGISTRATION, "same-token")
                .orElseThrow();
        registration.verify(now.plusSeconds(1));
        repository.save(registration);

        assertThat(repository
                        .findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, "same-token")
                        .orElseThrow()
                        .isVerified())
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void invalidatingOnePurposeLeavesTheOthersProofForTheSameUser(VerificationPurpose purpose) {
        VerificationPurpose other = other(purpose);
        repository.save(issue(purpose, 12L, "token-a", Duration.ofHours(1)));
        repository.save(issue(other, 12L, "token-b", Duration.ofHours(1)));

        repository.invalidateForUser(purpose, new UserId(12L));

        assertThat(repository.findByPresentedToken(purpose, "token-a")).isEmpty();
        assertThat(repository.findByPresentedToken(other, "token-b")).isPresent();
        assertThat(redis.hasKey(userKey(other, 12))).isTrue();
    }

    /** Pins that a proof written by the code before the purpose existed is still read, redeemed and invalidated. */
    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void aProofWrittenInTheExistingLayoutIsStillReadRedeemedAndInvalidated(VerificationPurpose purpose) {
        Instant expires = now.plusSeconds(3600);
        redis.opsForHash()
                .putAll(
                        tokenKey(purpose, "old-layout-token"),
                        Map.of(
                                "userId",
                                "13",
                                "email",
                                "old13@example.com",
                                "createdAt",
                                Long.toString(micros(now)),
                                "expiresAt",
                                Long.toString(micros(expires))));
        redis.opsForValue().set(userKey(purpose, 13), TokenDigest.sha256Hex("old-layout-token"));

        EmailVerification found =
                repository.findByPresentedToken(purpose, "old-layout-token").orElseThrow();
        assertThat(found.purpose()).isEqualTo(purpose);
        assertThat(found.userId()).isEqualTo(new UserId(13L));
        assertThat(found.email()).isEqualTo(new Email("old13@example.com"));
        assertThat(found.verify(now.plusSeconds(1))).isTrue();
        repository.save(found);
        assertThat(repository
                        .findByPresentedToken(purpose, "old-layout-token")
                        .orElseThrow()
                        .isVerified())
                .isTrue();

        redis.opsForHash()
                .putAll(
                        tokenKey(purpose, "old-layout-open"),
                        Map.of(
                                "userId",
                                "14",
                                "email",
                                "old14@example.com",
                                "createdAt",
                                Long.toString(micros(now)),
                                "expiresAt",
                                Long.toString(micros(expires))));
        redis.opsForValue().set(userKey(purpose, 14), TokenDigest.sha256Hex("old-layout-open"));

        repository.invalidateForUser(purpose, new UserId(14L));

        assertThat(repository.findByPresentedToken(purpose, "old-layout-open")).isEmpty();
    }

    private static VerificationPurpose other(VerificationPurpose purpose) {
        return purpose == VerificationPurpose.REGISTRATION
                ? VerificationPurpose.EMAIL_CHANGE
                : VerificationPurpose.REGISTRATION;
    }
}
