package com.minewaku.chatter.identityaccess.authentication.internal.model;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthenticationSessionTest {

    private static final String SESSION_ID = "0190f3a0-0000-7000-8000-000000000001";
    private static final Duration LIFETIME = Duration.ofDays(30);

    @Test
    void issues_session_with_exact_expiry_boundary_and_absent_last_use_and_revocation() {
        AuthenticationSession session =
                AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, LIFETIME);
        Instant expiry = NOW.plus(LIFETIME);

        assertEquals(expiry, session.expiresAt());
        assertEquals(null, session.lastUsedAt());
        assertEquals(null, session.revokedAt());
        assertTrue(session.isUsable(expiry.minusNanos(1)));
        assertFalse(session.isUsable(expiry));
        assertTrue(session.isExpired(expiry));
    }

    @Test
    void rejects_blank_or_long_device_and_nonpositive_lifetime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AuthenticationSession.issue(SESSION_ID, new UserId(42), " ", NOW, LIFETIME));
        assertThrows(
                IllegalArgumentException.class,
                () -> AuthenticationSession.issue(SESSION_ID, new UserId(42), "a".repeat(201), NOW, LIFETIME));
        assertThrows(
                IllegalArgumentException.class,
                () -> AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, Duration.ZERO));
    }

    @Test
    void revokes_once_and_keeps_first_revocation_time() {
        AuthenticationSession session =
                AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, LIFETIME);

        assertTrue(session.revoke(NOW.plusSeconds(5)));
        assertFalse(session.revoke(NOW.plusSeconds(10)));
        assertEquals(NOW.plusSeconds(5), session.revokedAt());
        assertFalse(session.isUsable(NOW.plusSeconds(6)));
    }

    @Test
    void last_use_moves_forward_only_and_revoked_session_cannot_be_used() {
        AuthenticationSession session =
                AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, LIFETIME);

        session.markUsed(NOW.plusSeconds(60));
        session.markUsed(NOW.plusSeconds(30));
        assertEquals(NOW.plusSeconds(60), session.lastUsedAt());

        session.revoke(NOW.plusSeconds(90));
        assertThrows(IllegalStateException.class, () -> session.markUsed(NOW.plusSeconds(120)));
    }

    @Test
    void renewal_gives_a_new_expiry_and_advances_last_use() {
        AuthenticationSession session =
                AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, LIFETIME);
        Instant renewedAt = NOW.plus(Duration.ofDays(29));

        session.renew(renewedAt, renewedAt.plus(LIFETIME));

        assertEquals(renewedAt.plus(LIFETIME), session.expiresAt());
        assertEquals(renewedAt, session.lastUsedAt());
        assertEquals(NOW, session.issuedAt());
    }

    @Test
    void renewal_has_no_upper_limit_on_total_age() {
        AuthenticationSession session =
                AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, LIFETIME);
        Instant at = NOW;
        for (int i = 0; i < 20; i++) {
            at = at.plus(Duration.ofDays(29));
            session.renew(at, at.plus(LIFETIME));
        }

        assertTrue(session.isUsable(at.plusSeconds(1)));
        assertEquals(at.plus(LIFETIME), session.expiresAt());
    }

    @Test
    void revoked_expired_or_backwards_renewal_is_rejected_and_changes_nothing() {
        AuthenticationSession session =
                AuthenticationSession.issue(SESSION_ID, new UserId(42), "Laptop", NOW, LIFETIME);
        Instant expiry = session.expiresAt();

        assertThrows(IllegalStateException.class, () -> session.renew(expiry, expiry.plus(LIFETIME)));
        assertThrows(IllegalArgumentException.class, () -> session.renew(NOW.plusSeconds(5), NOW.plusSeconds(5)));
        session.revoke(NOW.plusSeconds(10));
        assertThrows(IllegalStateException.class, () -> session.renew(NOW.plusSeconds(20), NOW.plusSeconds(30)));

        assertEquals(expiry, session.expiresAt());
        assertEquals(null, session.lastUsedAt());
    }
}
