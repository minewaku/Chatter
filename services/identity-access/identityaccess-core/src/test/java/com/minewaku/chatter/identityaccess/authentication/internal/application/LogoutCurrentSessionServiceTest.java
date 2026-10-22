package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.LogoutCurrentSessionService;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogoutCurrentSessionServiceTest {

    private static final String FIRST = "0190f3a0-0000-7000-8000-000000000001";
    private static final String SECOND = "0190f3a0-0000-7000-8000-000000000002";

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        sessions.seed(AuthenticationTestFakes.session(FIRST, 42, NOW), "refresh-1");
        sessions.seed(AuthenticationTestFakes.session(SECOND, 42, NOW), "refresh-2");
    }

    @Test
    void signs_out_once_and_leaves_other_sessions_usable() {
        LogoutCurrentSessionUseCase.Result first =
                service(NOW.plusSeconds(10)).handle(new LogoutCurrentSessionUseCase.Command("refresh-1"));
        LogoutCurrentSessionUseCase.Result second =
                service(NOW.plusSeconds(11)).handle(new LogoutCurrentSessionUseCase.Command("refresh-1"));

        assertTrue(first.revoked());
        assertFalse(second.revoked());
        assertTrue(sessions.get(SECOND).isUsable(NOW.plusSeconds(11)));
    }

    @Test
    void unknown_blank_and_expired_credentials_return_false() {
        Instant expired = NOW.plusSeconds(60L * 60 * 24 * 30);
        assertFalse(service(NOW)
                .handle(new LogoutCurrentSessionUseCase.Command("unknown"))
                .revoked());
        assertFalse(
                service(NOW).handle(new LogoutCurrentSessionUseCase.Command("")).revoked());
        assertFalse(service(expired)
                .handle(new LogoutCurrentSessionUseCase.Command("refresh-1"))
                .revoked());
        assertEquals(null, sessions.get(FIRST).revokedAt());
    }

    @Test
    void inactive_owner_returns_false_without_revoking() {
        users.findById(new UserId(42)).orElseThrow().suspend();

        assertFalse(service(NOW.plusSeconds(1))
                .handle(new LogoutCurrentSessionUseCase.Command("refresh-1"))
                .revoked());
        assertEquals(null, sessions.get(FIRST).revokedAt());
    }

    private LogoutCurrentSessionService service(Instant now) {
        return new LogoutCurrentSessionService(sessions, users, clockAt(now));
    }
}
