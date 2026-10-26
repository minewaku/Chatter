package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.PASSWORD;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.accountWithStatus;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutOtherSessionsUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.SessionUnusableException;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.LogoutOtherSessionsService;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.throttle.InMemoryAttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.TestPolicies;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.api.exception.UserAccountNotFoundException;
import com.minewaku.chatter.identityaccess.user.internal.application.ReauthenticationGuard;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LogoutOtherSessionsServiceTest {

    private static final String CURRENT = "0190f3a0-0000-7000-8000-000000000001";
    private static final String OTHER_ONE = "0190f3a0-0000-7000-8000-000000000002";
    private static final String OTHER_TWO = "0190f3a0-0000-7000-8000-000000000003";
    private static final String FOREIGN = "0190f3a0-0000-7000-8000-000000000004";

    private final InMemoryAttemptLimiter limiter = new InMemoryAttemptLimiter();
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
        assertThrows(InvalidCredentialsException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, "Wrong-password1")));
        assertNull(sessions.get(OTHER_ONE).revokedAt());
    }

    @Test
    void five_wrong_passwords_block_the_sixth_attempt_and_revoke_nothing() {
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () -> service(NOW)
                    .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, "Wrong-password1")));
        }

        assertThrows(TooManyAttemptsException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, PASSWORD)));

        assertNull(sessions.get(OTHER_ONE).revokedAt());
        assertTrue(sessions.get(CURRENT).isUsable(NOW));
    }

    @Test
    void foreign_unknown_or_unusable_current_session_cannot_authorize() {
        sessions.get(CURRENT).revoke(NOW.plusSeconds(1));

        assertThrows(SessionUnusableException.class, () -> service(NOW.plusSeconds(2))
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, PASSWORD)));
        assertThrows(SessionUnusableException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, FOREIGN, PASSWORD)));
        assertThrows(SessionUnusableException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(42, "0190f3a0-0000-7000-8000-0000000000ff", PASSWORD)));
        assertNull(sessions.get(OTHER_ONE).revokedAt());
    }

    @Test
    void an_expired_current_session_cannot_authorize() {
        assertThrows(SessionUnusableException.class, () -> service(NOW.plus(java.time.Duration.ofDays(31)))
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, PASSWORD)));
        assertNull(sessions.get(OTHER_ONE).revokedAt());
    }

    @Test
    void a_wrong_password_is_reported_before_an_unusable_session() {
        sessions.get(CURRENT).revoke(NOW.plusSeconds(1));

        assertThrows(InvalidCredentialsException.class, () -> service(NOW.plusSeconds(2))
                .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, "Wrong-password1")));
    }

    @Test
    void a_blocked_account_is_reported_before_an_unusable_session() {
        users.add(accountWithStatus(44, "blocked@example.com", AccountStatus.LOCKED));

        assertThrows(InvalidAccountStateTransitionException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(44, "0190f3a0-0000-7000-8000-0000000000ff", PASSWORD)));
    }

    @Test
    void wrong_password_and_a_password_failing_the_policy_both_change_zero_sessions() {
        for (String presented : new String[] {"Wrong-password1", "weak"}) {
            assertThrows(InvalidCredentialsException.class, () -> service(NOW)
                    .handle(new LogoutOtherSessionsUseCase.Command(42, CURRENT, presented)));
        }
        assertNull(sessions.get(OTHER_ONE).revokedAt());
        assertNull(sessions.get(OTHER_TWO).revokedAt());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void a_non_active_account_is_reported_as_such_whatever_password_is_sent_and_revokes_nothing(
            AccountStatus status) {
        users.add(accountWithStatus(44, "blocked@example.com", status));
        String current = "0190f3a0-0000-7000-8000-000000000044";
        sessions.seed(AuthenticationTestFakes.session(current, 44, NOW), "refresh-44");
        sessions.seed(AuthenticationTestFakes.session("0190f3a0-0000-7000-8000-000000000045", 44, NOW), "refresh-45");

        for (String presented : new String[] {PASSWORD, "Wrong-password1", "weak"}) {
            assertThrows(InvalidAccountStateTransitionException.class, () -> service(NOW)
                    .handle(new LogoutOtherSessionsUseCase.Command(44, current, presented)));
        }
        assertNull(sessions.get("0190f3a0-0000-7000-8000-000000000045").revokedAt());
    }

    @Test
    void a_missing_account_is_reported_as_missing() {
        assertThrows(UserAccountNotFoundException.class, () -> service(NOW)
                .handle(new LogoutOtherSessionsUseCase.Command(99, CURRENT, PASSWORD)));
    }

    private LogoutOtherSessionsService service(Instant now) {
        return new LogoutOtherSessionsService(
                users,
                new AuthenticationTestFakes.PasswordHasherFake(),
                sessions,
                clockAt(now),
                new ReauthenticationGuard(limiter, TestPolicies.defaults()));
    }
}
