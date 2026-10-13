package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EmailVerificationIssuanceCoordinatorTest {
    @Test
    void saves_and_delivers_same_token_and_expiry_after_invalidating_old_records() {
        var repository = new EmailVerificationTestFakes.Repository();
        var generator = new EmailVerificationTestFakes.Generator();
        var notifier = new EmailVerificationTestFakes.Notifier();
        var coordinator = new EmailVerificationIssuanceCoordinator(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), generator, repository, notifier);

        coordinator.issue(new UserId(1), new Email("person@example.com"));

        var saved = repository.records.get("token-1");
        var delivery = notifier.deliveries.getFirst();
        assertEquals("token-1", delivery.token());
        assertEquals(saved.expiresAt(), delivery.expiresAt());
        assertEquals(Instant.parse("2026-01-02T00:00:00Z"), delivery.expiresAt());
        assertEquals(java.util.List.of("invalidate", "save"), repository.calls);
        assertEquals(java.util.List.of("notify"), notifier.calls);
        assertThrows(
                IllegalArgumentException.class,
                () -> new EmailVerificationIssuanceCoordinator(
                        Clock.systemUTC(), Duration.ZERO, generator, repository, notifier));
    }

    @Test
    void propagates_generator_and_delivery_failures() {
        var generator = new EmailVerificationTestFakes.Generator();
        var repository = new EmailVerificationTestFakes.Repository();
        var notifier = new EmailVerificationTestFakes.Notifier();
        var coordinator = new EmailVerificationIssuanceCoordinator(Clock.systemUTC(), generator, repository, notifier);
        generator.failure = new IllegalStateException("generator");
        assertThrows(
                IllegalStateException.class, () -> coordinator.issue(new UserId(1), new Email("person@example.com")));
        generator.failure = null;
        repository.failure = new IllegalStateException("repository");
        assertThrows(
                IllegalStateException.class, () -> coordinator.issue(new UserId(1), new Email("person@example.com")));
        repository.failure = null;
        notifier.failure = new IllegalStateException("notifier");
        assertThrows(
                IllegalStateException.class, () -> coordinator.issue(new UserId(1), new Email("person@example.com")));
    }
}
