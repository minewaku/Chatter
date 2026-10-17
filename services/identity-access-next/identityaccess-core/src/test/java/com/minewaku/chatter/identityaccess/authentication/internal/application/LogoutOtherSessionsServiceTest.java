package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.PASSWORD;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutOtherSessionsUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.LogoutOtherSessionsService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogoutOtherSessionsServiceTest {

    private static final String CURRENT = "0190f3a0-0000-7000-8000-000000000001";
    private static final String OTHER_ONE = "0190f3a0-0000-7000-8000-000000000002";
    private static final String OTHER_TWO = "0190f3a0-0000-7000-8000-000000000003";
    private static final String FOREIGN = "0190f3a0-0000-7000-8000-000000000004";

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        sessions.seed(AuthenticationTestFakes.session(CURRENT, 42, NOW), "refresh-1");
        sessions.seed(AuthenticationTestFakes.session(OTHER_ONE, 42, NOW), "refresh-2");
        sessions.seed(AuthenticationTestFakes.session(OTHER_TWO, 42, NOW), "refresh-3");
        sessions.seed(AuthenticationTestFakes.session(FOREIGN, 43, NOW), "refresh-4");
    }

    @Test
    void revokes_other_owned_sessions_and_keeps_current_usable_then_counts_zero() {
        Instant later = NOW.plusSeconds(30);

        assertEquals(
                2,
                service(later)
                        .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, PASSWORD))
                        .revokedCount());
        assertEquals(
                0,
                service(later)
                        .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, PASSWORD))
                        .revokedCount());
        assertTrue(sessions.get(CURRENT).isUsable(later));
        assertNull(sessions.get(FOREIGN).revokedAt());
    }

    @Test
    void wrong_password_changes_zero_sessions() {
        assertThrows(AuthenticationFailedException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, "Wrong-password1")));
        assertNull(sessions.get(OTHER_ONE).revokedAt());
    }

    @Test
    void foreign_unknown_or_unusable_current_session_cannot_authorize() {
        sessions.get(CURRENT).revoke(NOW.plusSeconds(1));

        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, PASSWORD)));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, FOREIGN, PASSWORD)));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, "0190f3a0-0000-7000-8000-0000000000ff", PASSWORD)));
        assertNull(sessions.get(OTHER_ONE).revokedAt());
    }

    private LogoutOtherSessionsService service(Instant now) {
        return new LogoutOtherSessionsService(
                users, new AuthenticationTestFakes.PasswordHasherFake(), sessions, clockAt(now));
    }
}
