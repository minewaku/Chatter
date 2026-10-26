package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.PASSWORD;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.accountWithStatus;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.LogoutSessionService;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.throttle.InMemoryAttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.TestPolicies;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.api.exception.UserAccountNotFoundException;
import com.minewaku.chatter.identityaccess.user.internal.application.ReauthenticationGuard;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LogoutSessionServiceTest {

    private static final String OWNED = "0190f3a0-0000-7000-8000-000000000001";
    private static final String FOREIGN = "0190f3a0-0000-7000-8000-000000000002";

    private final InMemoryAttemptLimiter limiter = new InMemoryAttemptLimiter();
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
        assertThrows(InvalidCredentialsException.class, () -> service()
                .handle(new LogoutSessionUseCase.Command(42, OWNED, "Wrong-password1")));
        assertEquals(null, sessions.get(OWNED).revokedAt());
    }

    @Test
    void a_password_that_fails_the_policy_is_a_wrong_password() {
        assertThrows(InvalidCredentialsException.class, () -> service()
                .handle(new LogoutSessionUseCase.Command(42, OWNED, "weak")));
        assertEquals(null, sessions.get(OWNED).revokedAt());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void a_non_active_account_is_reported_as_such_whatever_password_is_sent(AccountStatus status) {
        users.add(accountWithStatus(44, "blocked@example.com", status));
        sessions.seed(AuthenticationTestFakes.session("0190f3a0-0000-7000-8000-000000000044", 44, NOW), "refresh-44");

        for (String password : new String[] {PASSWORD, "Wrong-password1", "weak"}) {
            assertThrows(InvalidAccountStateTransitionException.class, () -> service()
                    .handle(new LogoutSessionUseCase.Command(44, "0190f3a0-0000-7000-8000-000000000044", password)));
        }
        assertEquals(
                null, sessions.get("0190f3a0-0000-7000-8000-000000000044").revokedAt());
    }

    @Test
    void a_missing_account_is_reported_as_missing() {
        assertThrows(UserAccountNotFoundException.class, () -> service()
                .handle(new LogoutSessionUseCase.Command(99, OWNED, PASSWORD)));
    }

    @Test
    void five_wrong_passwords_block_the_sixth_attempt_and_revoke_nothing() {
        LogoutSessionService service = service();
        for (int i = 0; i < 5; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> service.handle(new LogoutSessionUseCase.Command(42, OWNED, "Wrong-password1")));
        }

        assertThrows(
                TooManyAttemptsException.class,
                () -> service.handle(new LogoutSessionUseCase.Command(42, OWNED, PASSWORD)));

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
                users,
                new AuthenticationTestFakes.PasswordHasherFake(),
                sessions,
                clockAt(NOW),
                new ReauthenticationGuard(limiter, TestPolicies.defaults()));
    }
}
