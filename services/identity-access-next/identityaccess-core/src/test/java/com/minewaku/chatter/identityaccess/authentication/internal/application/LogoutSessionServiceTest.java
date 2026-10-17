package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.PASSWORD;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.LogoutSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogoutSessionServiceTest {

    private static final String OWNED = "0190f3a0-0000-7000-8000-000000000001";
    private static final String FOREIGN = "0190f3a0-0000-7000-8000-000000000002";

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        sessions.seed(AuthenticationTestFakes.session(OWNED, 42, NOW), "refresh-1");
        sessions.seed(AuthenticationTestFakes.session(FOREIGN, 43, NOW), "refresh-2");
    }

    @Test
    void correct_password_revokes_owned_session_once() {
        LogoutSessionService service = service();

        assertTrue(service.handle(new LogoutSessionUseCase.Command(42, OWNED, PASSWORD))
                .revoked());
        assertFalse(service.handle(new LogoutSessionUseCase.Command(42, OWNED, PASSWORD))
                .revoked());
    }

    @Test
    void wrong_password_fails_and_changes_no_session() {
        assertThrows(AuthenticationFailedException.class, () -> service()
                .handle(new LogoutSessionUseCase.Command(42, OWNED, "Wrong-password1")));
        assertEquals(null, sessions.get(OWNED).revokedAt());
    }

    @Test
    void foreign_or_missing_session_returns_false_without_disclosure() {
        LogoutSessionService service = service();

        assertFalse(service.handle(new LogoutSessionUseCase.Command(42, FOREIGN, PASSWORD))
                .revoked());
        assertFalse(
                service.handle(new LogoutSessionUseCase.Command(42, "0190f3a0-0000-7000-8000-0000000000ff", PASSWORD))
                        .revoked());
        assertEquals(null, sessions.get(FOREIGN).revokedAt());
    }

    private LogoutSessionService service() {
        return new LogoutSessionService(
                users, new AuthenticationTestFakes.PasswordHasherFake(), sessions, clockAt(NOW));
    }
}
