package com.minewaku.chatter.identityaccess.emailverification.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EmailVerificationTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED, ZoneOffset.UTC);

    @Test
    void issue_captures_required_values_and_positive_lifetime() {
        EmailVerification verification = EmailVerification.issue(
                "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ofHours(24));

        assertEquals("secret", verification.rawToken());
        assertEquals(new UserId(1), verification.userId());
        assertEquals(new Email("person@example.com"), verification.email());
        assertEquals(CREATED, verification.createdAt());
        assertEquals(CREATED.plus(Duration.ofHours(24)), verification.expiresAt());
        assertNull(verification.verifiedAt());
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.issue(
                        "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ZERO));
    }

    @Test
    void expires_at_exact_boundary_and_consumes_once() {
        EmailVerification verification = EmailVerification.issue(
                "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ofHours(1));

        assertFalse(verification.isExpired(CREATED.plusSeconds(3599)));
        assertTrue(verification.isExpired(CREATED.plusSeconds(3600)));
        assertTrue(verification.verify(CREATED.plusSeconds(1)));
        assertFalse(verification.verify(CREATED.plusSeconds(2)));
        assertEquals(CREATED.plusSeconds(1), verification.verifiedAt());
    }

    @Test
    void reconstitutes_consumed_and_unused_verifications() {
        assertFalse(EmailVerification.reconstitute(
                        "unused",
                        new UserId(1),
                        new Email("person@example.com"),
                        CREATED,
                        CREATED.plusSeconds(10),
                        null)
                .isVerified());
        assertTrue(EmailVerification.reconstitute(
                        "used",
                        new UserId(1),
                        new Email("person@example.com"),
                        CREATED,
                        CREATED.plusSeconds(10),
                        CREATED)
                .isVerified());
    }
}
