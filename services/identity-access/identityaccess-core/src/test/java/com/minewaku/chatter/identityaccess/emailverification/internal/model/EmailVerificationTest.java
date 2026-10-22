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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EmailVerificationTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED, ZoneOffset.UTC);

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void issue_captures_required_values_and_positive_lifetime(VerificationPurpose purpose) {
        EmailVerification verification = EmailVerification.issue(
                purpose, "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ofHours(24));

        assertEquals(purpose, verification.purpose());
        assertEquals("secret", verification.rawToken());
        assertEquals(new UserId(1), verification.userId());
        assertEquals(new Email("person@example.com"), verification.email());
        assertEquals(CREATED, verification.createdAt());
        assertEquals(CREATED.plus(Duration.ofHours(24)), verification.expiresAt());
        assertNull(verification.verifiedAt());
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.issue(
                        purpose, "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ZERO));
    }

    @Test
    void rejects_a_missing_purpose() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> EmailVerification.issue(
                        null, "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ofHours(1)));
        assertEquals("purpose is required", error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void expires_at_exact_boundary_and_consumes_once(VerificationPurpose purpose) {
        EmailVerification verification = EmailVerification.issue(
                purpose, "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ofHours(1));

        assertFalse(verification.isExpired(CREATED.plusSeconds(3599)));
        assertTrue(verification.isExpired(CREATED.plusSeconds(3600)));
        assertTrue(verification.verify(CREATED.plusSeconds(1)));
        assertFalse(verification.verify(CREATED.plusSeconds(2)));
        assertEquals(CREATED.plusSeconds(1), verification.verifiedAt());
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void reconstitutes_consumed_and_unused_verifications(VerificationPurpose purpose) {
        assertFalse(EmailVerification.reconstitute(
                        purpose,
                        "unused",
                        new UserId(1),
                        new Email("person@example.com"),
                        CREATED,
                        CREATED.plusSeconds(10),
                        null)
                .isVerified());
        assertTrue(EmailVerification.reconstitute(
                        purpose,
                        "used",
                        new UserId(1),
                        new Email("person@example.com"),
                        CREATED,
                        CREATED.plusSeconds(10),
                        CREATED)
                .isVerified());
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void a_negative_lifetime_is_rejected(VerificationPurpose purpose) {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.issue(
                        purpose,
                        "secret",
                        new UserId(1),
                        new Email("person@example.com"),
                        CLOCK,
                        Duration.ofSeconds(-1)));
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void a_token_user_and_email_are_required(VerificationPurpose purpose) {
        Email email = new Email("person@example.com");
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.issue(purpose, " ", new UserId(1), email, CLOCK, Duration.ofHours(1)));
        assertThrows(
                NullPointerException.class,
                () -> EmailVerification.issue(purpose, "secret", null, email, CLOCK, Duration.ofHours(1)));
        assertThrows(
                NullPointerException.class,
                () -> EmailVerification.issue(purpose, "secret", new UserId(1), null, CLOCK, Duration.ofHours(1)));
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void verifying_never_works_after_expiry(VerificationPurpose purpose) {
        EmailVerification late = EmailVerification.issue(
                purpose, "secret", new UserId(1), new Email("person@example.com"), CLOCK, Duration.ofHours(24));

        assertFalse(late.verify(CREATED.plus(Duration.ofHours(24))));
        assertFalse(late.isVerified());
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void reconstituting_rejects_a_verification_outside_the_lifetime(VerificationPurpose purpose) {
        Instant expires = CREATED.plus(Duration.ofHours(1));
        UserId user = new UserId(1);
        Email email = new Email("person@example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.reconstitute(
                        purpose, "secret", user, email, CREATED, expires, CREATED.minusSeconds(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.reconstitute(purpose, "secret", user, email, CREATED, expires, expires));
        assertThrows(
                IllegalArgumentException.class,
                () -> EmailVerification.reconstitute(purpose, "secret", user, email, expires, CREATED, null));
    }
}
