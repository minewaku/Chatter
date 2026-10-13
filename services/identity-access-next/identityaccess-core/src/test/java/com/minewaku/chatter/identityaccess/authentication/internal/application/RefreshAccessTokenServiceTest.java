package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.minewaku.chatter.identityaccess.authentication.api.command.RefreshAccessTokenUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.RefreshAccessTokenService;
import com.minewaku.chatter.identityaccess.authentication.internal.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshAccessTokenServiceTest {

    private static final String SESSION_ID = "0190f3a0-0000-7000-8000-000000000001";

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();
    private final AuthenticationTestFakes.RecordingAccessTokenIssuer accessTokens =
            new AuthenticationTestFakes.RecordingAccessTokenIssuer();

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        sessions.seed(AuthenticationTestFakes.session(SESSION_ID, 42, NOW), "refresh-1");
    }

    @Test
    void renews_repeatedly_with_same_refresh_credential_without_new_session_and_advances_last_use() {
        Instant first = NOW.plusSeconds(60);
        Instant second = NOW.plusSeconds(120);

        RefreshAccessTokenUseCase.Refreshed a =
                service(first).handle(new RefreshAccessTokenUseCase.Command("refresh-1"));
        RefreshAccessTokenUseCase.Refreshed b =
                service(second).handle(new RefreshAccessTokenUseCase.Command("refresh-1"));

        assertEquals("access-1", a.accessToken());
        assertEquals("access-2", b.accessToken());
        assertEquals(0, sessions.createCalls);
        AuthenticationSession session =
                sessions.findByPresentedToken("refresh-1").orElseThrow();
        assertEquals(SESSION_ID, session.sessionId());
        assertEquals(second, session.lastUsedAt());
    }

    @Test
    void exact_expiry_boundary_and_revoked_or_unknown_or_blank_credentials_fail() {
        Instant expiry = NOW.plus(Duration.ofDays(30));
        assertThrows(AuthenticationFailedException.class, () -> service(expiry)
                .handle(new RefreshAccessTokenUseCase.Command("refresh-1")));

        sessions.get(SESSION_ID).revoke(NOW.plusSeconds(1));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-1")));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command("unknown")));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command(" ")));
        assertEquals(0, accessTokens.issueCalls);
    }

    @Test
    void inaccessible_owner_fails_without_issuing_access() {
        UserAccount account = users.findById(new com.minewaku.chatter.identityaccess.user.internal.model.UserId(42))
                .orElseThrow();
        account.suspend();

        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(60))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-1")));
        assertEquals(0, accessTokens.issueCalls);
    }

    @Test
    void issuer_failure_propagates_and_leaves_last_use_unchanged() {
        accessTokens.failOnIssue = true;

        assertThrows(IllegalStateException.class, () -> service(NOW.plusSeconds(60))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-1")));
        assertEquals(
                null, sessions.findByPresentedToken("refresh-1").orElseThrow().lastUsedAt());
    }

    private RefreshAccessTokenService service(Instant now) {
        return new RefreshAccessTokenService(sessions, users, accessTokens, clockAt(now));
    }
}
