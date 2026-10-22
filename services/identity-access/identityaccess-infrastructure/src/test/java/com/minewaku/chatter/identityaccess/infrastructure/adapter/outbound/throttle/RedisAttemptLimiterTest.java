package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.throttle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptOutcome;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisAttemptLimiterTest extends RedisTestSupport {

    private static final String PREFIX = "limiter-test:";
    private static final AttemptPolicy THREE = new AttemptPolicy("three", 3, Duration.ofMinutes(10));
    private static final AttemptPolicy OTHER = new AttemptPolicy("other", 3, Duration.ofMinutes(10));

    private StringRedisTemplate redis;
    private CircuitBreaker breaker;
    private RedisAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        redis = newTemplate();
        flushAll(redis);
        breaker = newBreaker();
        limiter = new RedisAttemptLimiter(redis, new RedisKeyProperties(PREFIX), breaker);
    }

    private static CircuitBreaker newBreaker() {
        return CircuitBreaker.of(
                "test",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(2)
                        .minimumNumberOfCalls(2)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(7))
                        .build());
    }

    private String keyOf(AttemptPolicy policy, String subject) {
        return PREFIX + "throttle:" + policy.name() + ":" + TokenDigest.sha256Hex(subject);
    }

    @Test
    void allowsUpToTheMaximumAndRefusesTheNextWithAWaitNoLongerThanTheWindow() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.hit(THREE, "a@example.com").allowed()).isTrue();
        }

        AttemptOutcome refused = limiter.hit(THREE, "a@example.com");

        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfter()).isPositive().isLessThanOrEqualTo(THREE.window());
    }

    @Test
    void aRefusedAttemptDoesNotExtendTheWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.hit(THREE, "a@example.com");
        }
        Long before = redis.getExpire(keyOf(THREE, "a@example.com"), TimeUnit.MILLISECONDS);

        limiter.hit(THREE, "a@example.com");
        limiter.hit(THREE, "a@example.com");
        Long after = redis.getExpire(keyOf(THREE, "a@example.com"), TimeUnit.MILLISECONDS);

        assertThat(before).isPositive();
        assertThat(after).isPositive().isLessThanOrEqualTo(before);
    }

    @Test
    void clearForgetsTheCount() {
        for (int i = 0; i < 4; i++) {
            limiter.hit(THREE, "a@example.com");
        }

        limiter.clear(THREE, "a@example.com");

        assertThat(redis.hasKey(keyOf(THREE, "a@example.com"))).isFalse();
        assertThat(limiter.hit(THREE, "a@example.com").allowed()).isTrue();
    }

    @Test
    void subjectsAndPoliciesAreIndependent() {
        for (int i = 0; i < 4; i++) {
            limiter.hit(THREE, "a@example.com");
        }

        assertThat(limiter.hit(THREE, "b@example.com").allowed()).isTrue();
        assertThat(limiter.hit(OTHER, "a@example.com").allowed()).isTrue();
    }

    @Test
    void theWindowEndsOnItsOwn() throws InterruptedException {
        AttemptPolicy shortWindow = new AttemptPolicy("short", 1, Duration.ofMillis(600));
        limiter.hit(shortWindow, "a@example.com");
        assertThat(limiter.hit(shortWindow, "a@example.com").allowed()).isFalse();

        Thread.sleep(900);

        assertThat(limiter.hit(shortWindow, "a@example.com").allowed()).isTrue();
    }

    @Test
    void aCounterWithoutAnExpiryGetsOne() {
        redis.opsForValue().set(keyOf(THREE, "a@example.com"), "1");

        limiter.hit(THREE, "a@example.com");

        assertThat(redis.getExpire(keyOf(THREE, "a@example.com"), TimeUnit.MILLISECONDS))
                .isPositive();
    }

    @Test
    void theStoredKeyContainsNoPartOfTheRawSubject() {
        limiter.hit(THREE, "alice.secret@example.com");

        Set<String> keys = redis.keys(PREFIX + "*");

        assertThat(keys).hasSize(1);
        assertThat(keys.iterator().next())
                .doesNotContain("alice")
                .doesNotContain("example")
                .doesNotContain("secret");
    }

    @Test
    void concurrentHitsNeverExceedTheLimit() throws Exception {
        AttemptPolicy ten = new AttemptPolicy("ten", 10, Duration.ofMinutes(10));
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<Boolean>> hits = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                hits.add(() -> limiter.hit(ten, "race@example.com").allowed());
            }
            long allowed = 0;
            for (Future<Boolean> result : pool.invokeAll(hits)) {
                if (result.get()) {
                    allowed++;
                }
            }
            assertThat(allowed).isEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void anUnreachableStoreFailsClosed() {
        LettuceConnectionFactory impatient = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redisHost(), redisPort()),
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofMillis(500))
                        .build());
        impatient.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(impatient);
        template.afterPropertiesSet();
        RedisAttemptLimiter impatientLimiter =
                new RedisAttemptLimiter(template, new RedisKeyProperties(PREFIX), newBreaker());

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            assertThatThrownBy(() -> impatientLimiter.hit(THREE, "a@example.com"))
                    .isInstanceOf(DependencyUnavailableException.class);
            assertThatThrownBy(() -> impatientLimiter.clear(THREE, "a@example.com"))
                    .isInstanceOf(DependencyUnavailableException.class);
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
            impatient.destroy();
        }
    }

    @Test
    void anOpenBreakerFailsWithoutTouchingTheStoreAndReportsTheHoldOff() {
        StringRedisTemplate untouched = mock(StringRedisTemplate.class);
        CircuitBreaker open = newBreaker();
        open.transitionToOpenState();
        RedisAttemptLimiter held = new RedisAttemptLimiter(untouched, new RedisKeyProperties(PREFIX), open);

        assertThatThrownBy(() -> held.hit(THREE, "a@example.com"))
                .isInstanceOfSatisfying(DependencyUnavailableException.class, e -> assertThat(e.retryAfter())
                        .isEqualTo(Duration.ofSeconds(7)));
        verifyNoInteractions(untouched);
    }
}
