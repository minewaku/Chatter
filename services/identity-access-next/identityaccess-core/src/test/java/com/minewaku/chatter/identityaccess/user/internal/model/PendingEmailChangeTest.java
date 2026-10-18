package com.minewaku.chatter.identityaccess.user.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PendingEmailChangeTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UserId USER = new UserId(42);
    private static final Email NEW_EMAIL = new Email("new@example.com");

    private PendingEmailChange issue() {
        return PendingEmailChange.issue("raw-token", USER, NEW_EMAIL, CLOCK, Duration.ofHours(24));
    }

    @Test
    void issuing_stamps_the_lifetime_from_the_clock_and_starts_unconfirmed() {
        PendingEmailChange change = issue();

        assertEquals(NOW, change.createdAt());
        assertEquals(NOW.plus(Duration.ofHours(24)), change.expiresAt());
        assertEquals(NEW_EMAIL, change.newEmail());
        assertEquals(USER, change.userId());
        assertEquals("raw-token", change.rawToken());
        assertFalse(change.isConfirmed());
        assertNull(change.confirmedAt());
    }

    @Test
    void a_lifetime_must_be_positive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PendingEmailChange.issue("raw-token", USER, NEW_EMAIL, CLOCK, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> PendingEmailChange.issue("raw-token", USER, NEW_EMAIL, CLOCK, Duration.ofSeconds(-1)));
    }

    @Test
    void a_token_user_and_email_are_required() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PendingEmailChange.issue(" ", USER, NEW_EMAIL, CLOCK, Duration.ofHours(1)));
        assertThrows(
                NullPointerException.class,
                () -> PendingEmailChange.issue("raw-token", null, NEW_EMAIL, CLOCK, Duration.ofHours(1)));
        assertThrows(
                NullPointerException.class,
                () -> PendingEmailChange.issue("raw-token", USER, null, CLOCK, Duration.ofHours(1)));
    }

    @Test
    void confirming_works_once_and_never_after_expiry() {
        PendingEmailChange change = issue();

        assertTrue(change.confirm(NOW.plusSeconds(1)));
        assertTrue(change.isConfirmed());
        assertFalse(change.confirm(NOW.plusSeconds(2)));

        PendingEmailChange late = issue();
        assertFalse(late.confirm(NOW.plus(Duration.ofHours(24))));
        assertFalse(late.isConfirmed());
    }

    @Test
    void expiry_is_reached_exactly_at_the_expiry_instant() {
        PendingEmailChange change = issue();

        assertFalse(change.isExpired(NOW.plus(Duration.ofHours(24)).minusMillis(1)));
        assertTrue(change.isExpired(NOW.plus(Duration.ofHours(24))));
    }

    @Test
    void reconstituting_rejects_a_confirmation_outside_the_lifetime() {
        Instant expires = NOW.plus(Duration.ofHours(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> PendingEmailChange.reconstitute("raw-token", USER, NEW_EMAIL, NOW, expires, NOW.minusSeconds(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> PendingEmailChange.reconstitute("raw-token", USER, NEW_EMAIL, NOW, expires, expires));
        assertThrows(
                IllegalArgumentException.class,
                () -> PendingEmailChange.reconstitute("raw-token", USER, NEW_EMAIL, expires, NOW, null));
        assertTrue(PendingEmailChange.reconstitute("raw-token", USER, NEW_EMAIL, NOW, expires, NOW.plusSeconds(1))
                .isConfirmed());
    }
}
