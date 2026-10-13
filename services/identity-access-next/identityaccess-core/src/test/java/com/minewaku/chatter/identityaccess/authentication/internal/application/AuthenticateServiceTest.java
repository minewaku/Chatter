package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.PASSWORD;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.AuthenticateUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.AuthenticateService;
import com.minewaku.chatter.identityaccess.authentication.internal.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticateServiceTest {

    private static final String FIRST_SESSION_ID = "0190f3a0-0000-7000-8000-000000000001";

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();
    private final AuthenticationTestFakes.RecordingAccessTokenIssuer accessTokens =
            new AuthenticationTestFakes.RecordingAccessTokenIssuer();
    private AuthenticateService service;

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        service = newService();
    }

    @Test
    void signs_in_active_account_with_one_session_and_both_credentials() {
        AuthenticateUseCase.Authenticated result = service.handle(command("  PERSON@example.com ", PASSWORD, "Laptop"));

        assertEquals("access-1", result.accessToken());
        assertEquals("refresh-1", result.refreshToken());
        assertEquals(1, sessions.createCalls);
        AuthenticationSession session =
                sessions.findByPresentedToken("refresh-1").orElseThrow();
        assertEquals(FIRST_SESSION_ID, session.sessionId());
        assertEquals(new UserId(42), session.userId());
        assertEquals(NOW, session.issuedAt());
        assertEquals(NOW.plus(Duration.ofDays(30)), session.expiresAt());
    }

    @Test
    void denies_absent_wrong_password_and_malformed_input_without_credentials_or_session() {
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.handle(command("nobody@example.com", PASSWORD, "Laptop")));
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.handle(command("person@example.com", "Wrong-password1", "Laptop")));
        assertThrows(
                AuthenticationFailedException.class, () -> service.handle(command("not-an-email", PASSWORD, "Laptop")));
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.handle(command("person@example.com", "short", "Laptop")));

        assertEquals(0, sessions.createCalls);
        assertEquals(0, accessTokens.issueCalls);
    }

    @Test
    void denies_every_inaccessible_status_without_session() {
        UserAccount locked = activeAccount(43, "locked@example.com");
        locked.lock();
        UserAccount suspended = activeAccount(44, "suspended@example.com");
        suspended.suspend();
        UserAccount deleted = activeAccount(45, "deleted@example.com");
        deleted.softDelete();
        UserAccount pending = UserAccount.register(
                new UserId(46),
                new Email("pending@example.com"),
                new Username("pending_46"),
                new Birthday(LocalDate.of(1990, 1, 1)),
                new AuthenticationTestFakes.PasswordHasherFake().hash(new PlainPassword(PASSWORD)));
        users.add(locked);
        users.add(suspended);
        users.add(deleted);
        users.add(pending);

        for (String email : new String[] {"locked@example.com", "suspended@example.com", "pending@example.com"}) {
            assertThrows(AuthenticationFailedException.class, () -> service.handle(command(email, PASSWORD, "Laptop")));
        }
        assertEquals(AccountStatus.DELETED, deleted.status());
        assertEquals(0, sessions.createCalls);
    }

    @Test
    void device_label_boundaries_are_enforced() {
        assertThrows(IllegalArgumentException.class, () -> new AuthenticateUseCase.DeviceInfo(" "));
        assertThrows(IllegalArgumentException.class, () -> new AuthenticateUseCase.DeviceInfo("a".repeat(201)));
        assertTrue(new AuthenticateUseCase.DeviceInfo("a".repeat(200)).label().length() == 200);
    }

    @Test
    void access_issuer_or_session_failure_never_reports_success() {
        accessTokens.failOnIssue = true;
        assertThrows(
                IllegalStateException.class, () -> service.handle(command("person@example.com", PASSWORD, "Laptop")));
        accessTokens.failOnIssue = false;

        sessions.failOnCreate = true;
        assertThrows(
                IllegalStateException.class, () -> service.handle(command("person@example.com", PASSWORD, "Laptop")));
        assertEquals(0, sessions.createCalls);
    }

    private AuthenticateService newService() {
        return new AuthenticateService(
                users,
                new AuthenticationTestFakes.PasswordHasherFake(),
                new AuthenticationTestFakes.SequenceSessionIds(),
                new AuthenticationTestFakes.SequenceRefreshTokens(),
                accessTokens,
                sessions,
                clockAt(NOW),
                Duration.ofDays(30));
    }

    private static AuthenticateUseCase.Command command(String email, String password, String device) {
        return new AuthenticateUseCase.Command(email, password, new AuthenticateUseCase.DeviceInfo(device));
    }
}
