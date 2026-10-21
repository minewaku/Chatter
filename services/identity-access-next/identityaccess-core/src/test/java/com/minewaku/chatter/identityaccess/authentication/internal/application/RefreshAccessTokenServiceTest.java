package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.clockAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.command.RefreshAccessTokenUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.application.command.RefreshAccessTokenService;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshAccessTokenServiceTest {

    private static final String SESSION_ID = "0190f3a0-0000-7000-8000-000000000001";
    private static final Duration LIFETIME = Duration.ofDays(30);

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();
    private final AuthenticationTestFakes.RecordingAccessTokenIssuer accessTokens =
            new AuthenticationTestFakes.RecordingAccessTokenIssuer();
    private final AuthenticationTestFakes.SequenceRefreshTokens refreshTokens =
            new AuthenticationTestFakes.SequenceRefreshTokens();

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        sessions.seed(AuthenticationTestFakes.session(SESSION_ID, 42, NOW), "refresh-seed");
    }

    @Test
    void renewal_returns_both_credentials_and_gives_the_session_a_new_full_lifetime() {
        Instant at = NOW.plusSeconds(60);

        RefreshAccessTokenUseCase.Refreshed result =
                service(at).handle(new RefreshAccessTokenUseCase.Command("refresh-seed"));

        assertEquals("access-1", result.accessToken());
        assertEquals("refresh-1", result.refreshToken());
        assertEquals(SESSION_ID, accessTokens.lastSessionId);
        assertEquals(0, sessions.createCalls);
        AuthenticationSession session =
                sessions.findByPresentedToken("refresh-1").orElseThrow();
        assertEquals(SESSION_ID, session.sessionId());
        assertEquals(at.plus(LIFETIME), session.expiresAt());
        assertEquals(at, session.lastUsedAt());
    }

    @Test
    void the_presented_credential_stops_working_once_exchanged() {
        service(NOW.plusSeconds(60)).handle(new RefreshAccessTokenUseCase.Command("refresh-seed"));

        assertTrue(sessions.findByPresentedToken("refresh-seed").isEmpty());
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(120))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));
    }

    @Test
    void renewal_chains_and_a_continuously_used_session_has_no_maximum_age() {
        String credential = "refresh-seed";
        Instant at = NOW;
        for (int i = 0; i < 15; i++) {
            at = at.plus(Duration.ofDays(29));
            RefreshAccessTokenUseCase.Refreshed result =
                    service(at).handle(new RefreshAccessTokenUseCase.Command(credential));
            assertNotEquals(credential, result.refreshToken());
            credential = result.refreshToken();
        }

        AuthenticationSession session =
                sessions.findByPresentedToken(credential).orElseThrow();
        assertEquals(SESSION_ID, session.sessionId());
        assertEquals(at.plus(LIFETIME), session.expiresAt());
        assertEquals(NOW, session.issuedAt());
    }

    @Test
    void failed_rotation_is_a_generic_failure() {
        sessions.failRotate = true;

        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(60))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));
        assertEquals(1, sessions.rotateCalls);
        assertTrue(sessions.findByPresentedToken("refresh-seed").isPresent());
    }

    @Test
    void exact_expiry_boundary_and_revoked_or_unknown_or_blank_credentials_fail() {
        Instant expiry = NOW.plus(LIFETIME);
        assertThrows(AuthenticationFailedException.class, () -> service(expiry)
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));

        sessions.get(SESSION_ID).revoke(NOW.plusSeconds(1));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command("unknown")));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command(" ")));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(2))
                .handle(new RefreshAccessTokenUseCase.Command(null)));
        assertEquals(0, accessTokens.issueCalls);
        assertEquals(0, sessions.rotateCalls);
    }

    @Test
    void inaccessible_owner_fails_without_issuing_access_or_rotating() {
        UserAccount account = users.findById(new com.minewaku.chatter.identityaccess.user.internal.model.UserId(42))
                .orElseThrow();
        account.suspend();

        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(60))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));
        assertEquals(0, accessTokens.issueCalls);
        assertEquals(0, sessions.rotateCalls);
    }

    @Test
    void issuer_failure_propagates_and_leaves_the_session_and_credential_unchanged() {
        accessTokens.failOnIssue = true;

        assertThrows(IllegalStateException.class, () -> service(NOW.plusSeconds(60))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));

        AuthenticationSession session =
                sessions.findByPresentedToken("refresh-seed").orElseThrow();
        assertEquals(null, session.lastUsedAt());
        assertEquals(NOW.plus(LIFETIME), session.expiresAt());
        assertEquals(0, sessions.rotateCalls);
    }

    // ---- reuse of a replaced credential (spec 009, user story 1) ----

    private static final String OTHER_SESSION_ID = "0190f3a0-0000-7000-8000-000000000002";

    private String renew(String credential, Instant at) {
        return service(at)
                .handle(new RefreshAccessTokenUseCase.Command(credential))
                .refreshToken();
    }

    private AuthenticationFailedException refused(String credential, Instant at) {
        return assertThrows(AuthenticationFailedException.class, () -> service(at)
                .handle(new RefreshAccessTokenUseCase.Command(credential)));
    }

    @Test
    void a_replaced_credential_is_refused_and_revokes_its_session() {
        renew("refresh-seed", NOW.plusSeconds(60));

        refused("refresh-seed", NOW.plusSeconds(120));

        assertEquals(1, sessions.revokeOnReuseCalls);
        assertEquals(NOW.plusSeconds(120), sessions.get(SESSION_ID).revokedAt());
        assertTrue(sessions.findByPresentedToken("refresh-1").isPresent());
        refused("refresh-1", NOW.plusSeconds(180));
    }

    @Test
    void every_earlier_credential_of_a_session_counts_as_reuse_not_only_the_latest() {
        String second = renew("refresh-seed", NOW.plusSeconds(60));
        String third = renew(second, NOW.plusSeconds(120));
        renew(third, NOW.plusSeconds(180));

        refused("refresh-seed", NOW.plusSeconds(240));

        assertEquals(NOW.plusSeconds(240), sessions.get(SESSION_ID).revokedAt());
    }

    @Test
    void the_refusal_for_a_replaced_credential_is_the_same_as_for_an_unknown_one() {
        renew("refresh-seed", NOW.plusSeconds(60));

        AuthenticationFailedException replaced = refused("refresh-seed", NOW.plusSeconds(120));
        AuthenticationFailedException unknown = refused("never-issued", NOW.plusSeconds(120));

        assertEquals(unknown.getClass(), replaced.getClass());
        assertEquals(unknown.getMessage(), replaced.getMessage());
    }

    @Test
    void reuse_revokes_only_its_own_session_and_leaves_the_accounts_other_sessions_usable() {
        sessions.seed(AuthenticationTestFakes.session(OTHER_SESSION_ID, 42, NOW), "refresh-other");
        renew("refresh-seed", NOW.plusSeconds(60));

        refused("refresh-seed", NOW.plusSeconds(120));

        assertEquals(null, sessions.get(OTHER_SESSION_ID).revokedAt());
        assertTrue(sessions.get(OTHER_SESSION_ID).isUsable(NOW.plusSeconds(121)));
        renew("refresh-other", NOW.plusSeconds(130));
    }

    @Test
    void reuse_of_a_credential_whose_session_is_already_revoked_or_expired_changes_nothing() {
        renew("refresh-seed", NOW.plusSeconds(60));
        sessions.get(SESSION_ID).revoke(NOW.plusSeconds(90));

        refused("refresh-seed", NOW.plusSeconds(120));

        assertEquals(NOW.plusSeconds(90), sessions.get(SESSION_ID).revokedAt());

        sessions.seed(AuthenticationTestFakes.session(OTHER_SESSION_ID, 42, NOW), "other-seed");
        renew("other-seed", NOW.plusSeconds(60));
        Instant afterExpiry = NOW.plusSeconds(60).plus(LIFETIME).plusSeconds(1);

        refused("other-seed", afterExpiry);

        assertEquals(null, sessions.get(OTHER_SESSION_ID).revokedAt());
    }

    @Test
    void a_failing_session_store_during_the_reuse_check_propagates_and_issues_nothing() {
        renew("refresh-seed", NOW.plusSeconds(60));
        int issuedBefore = accessTokens.issueCalls;
        sessions.failOnRevokeOnReuse = true;

        assertThrows(IllegalStateException.class, () -> service(NOW.plusSeconds(120))
                .handle(new RefreshAccessTokenUseCase.Command("refresh-seed")));

        assertEquals(issuedBefore, accessTokens.issueCalls);
    }

    // ---- normal use is unaffected (spec 009, user story 2) ----

    @Test
    void a_successful_renewal_makes_no_reuse_check() {
        String credential = "refresh-seed";
        for (int i = 1; i <= 5; i++) {
            credential = renew(credential, NOW.plusSeconds(60L * i));
        }

        assertEquals(0, sessions.revokeOnReuseCalls);
        assertEquals(null, sessions.get(SESSION_ID).revokedAt());
    }

    @Test
    void blank_and_never_issued_credentials_revoke_no_session() {
        sessions.seed(AuthenticationTestFakes.session(OTHER_SESSION_ID, 42, NOW), "refresh-other");

        refused("never-issued", NOW.plusSeconds(60));
        refused(" ", NOW.plusSeconds(60));
        assertThrows(AuthenticationFailedException.class, () -> service(NOW.plusSeconds(60))
                .handle(new RefreshAccessTokenUseCase.Command(null)));

        assertEquals(null, sessions.get(SESSION_ID).revokedAt());
        assertEquals(null, sessions.get(OTHER_SESSION_ID).revokedAt());
    }

    @Test
    void the_loser_of_a_simultaneous_renewal_is_refused_without_a_reuse_check_and_the_session_survives() {
        sessions.failRotate = true;

        refused("refresh-seed", NOW.plusSeconds(60));

        assertEquals(0, sessions.revokeOnReuseCalls);
        assertEquals(null, sessions.get(SESSION_ID).revokedAt());
        sessions.failRotate = false;
        renew("refresh-seed", NOW.plusSeconds(120));
    }

    @Test
    void an_inaccessible_owner_is_refused_without_a_reuse_check() {
        UserAccount account = users.findById(new com.minewaku.chatter.identityaccess.user.internal.model.UserId(42))
                .orElseThrow();
        account.suspend();

        refused("refresh-seed", NOW.plusSeconds(60));

        assertEquals(0, sessions.revokeOnReuseCalls);
    }

    private RefreshAccessTokenService service(Instant now) {
        return new RefreshAccessTokenService(sessions, users, accessTokens, refreshTokens, clockAt(now), LIFETIME);
    }
}
