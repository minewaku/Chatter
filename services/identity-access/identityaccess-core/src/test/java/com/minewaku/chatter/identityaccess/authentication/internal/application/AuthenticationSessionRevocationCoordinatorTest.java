package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import org.junit.jupiter.api.Test;

class AuthenticationSessionRevocationCoordinatorTest {

    @Test
    void revokes_all_owned_retained_sessions_at_supplied_time_and_counts_changes() {
        AuthenticationTestFakes.InMemorySessionRepository sessions =
                new AuthenticationTestFakes.InMemorySessionRepository();
        sessions.seed(AuthenticationTestFakes.session(id(1), 42, NOW), "r1");
        sessions.seed(AuthenticationTestFakes.session(id(2), 42, NOW), "r2");
        sessions.seed(AuthenticationTestFakes.session(id(3), 43, NOW), "r3");
        AuthenticationSessionRevocationCoordinator coordinator =
                new AuthenticationSessionRevocationCoordinator(sessions, AuthenticationTestFakes.clockAt(NOW));

        assertEquals(2, coordinator.revokeAllFor(new UserId(42)));
        assertEquals(0, coordinator.revokeAllFor(new UserId(42)));
        assertEquals(NOW, sessions.get(id(1)).revokedAt());
        assertNull(sessions.get(id(3)).revokedAt());
    }

    @Test
    void propagates_repository_failure() {
        AuthenticationTestFakes.InMemorySessionRepository sessions =
                new AuthenticationTestFakes.InMemorySessionRepository();
        sessions.failOnRevokeAll = true;
        AuthenticationSessionRevocationCoordinator coordinator =
                new AuthenticationSessionRevocationCoordinator(sessions, AuthenticationTestFakes.clockAt(NOW));

        assertThrows(IllegalStateException.class, () -> coordinator.revokeAllFor(new UserId(42)));
    }

    private static String id(int n) {
        return String.format("0190f3a0-0000-7000-8000-%012d", n);
    }
}
