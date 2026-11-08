package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.internal.application.command.PromotionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromotionPolicyTest {

    private static final Instant CREATED = Instant.parse("2026-11-04T10:00:00Z");

    private final PromotionPolicy policy = new PromotionPolicy(
            Duration.ofHours(2),
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30)),
            10,
            Duration.ofMinutes(5));

    @Test
    void delaysGrowThenTheLastOneRepeats() {
        assertThat(policy.delayBeforeRetry(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(policy.delayBeforeRetry(2)).isEqualTo(Duration.ofMinutes(5));
        assertThat(policy.delayBeforeRetry(3)).isEqualTo(Duration.ofMinutes(30));
        assertThat(policy.delayBeforeRetry(4)).isEqualTo(Duration.ofMinutes(30));
        assertThat(policy.delayBeforeRetry(500)).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void theHorizonIsReachedExactlyTwoHoursAfterCreation() {
        assertThat(policy.expired(CREATED, CREATED.plus(Duration.ofHours(2)).minusSeconds(1))).isFalse();
        assertThat(policy.expired(CREATED, CREATED.plus(Duration.ofHours(2)))).isTrue();
        assertThat(policy.expired(CREATED, CREATED.plus(Duration.ofHours(3)))).isTrue();
    }
}
