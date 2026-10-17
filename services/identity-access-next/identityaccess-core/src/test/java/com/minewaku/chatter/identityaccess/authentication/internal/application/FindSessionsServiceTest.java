package com.minewaku.chatter.identityaccess.authentication.internal.application;

import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.NOW;
import static com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes.activeAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.authentication.internal.application.query.FindSessionsService;
import java.lang.reflect.RecordComponent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindSessionsServiceTest {

    private final AuthenticationTestFakes.InMemoryUserAccounts users =
            new AuthenticationTestFakes.InMemoryUserAccounts();
    private final AuthenticationTestFakes.InMemorySessionRepository sessions =
            new AuthenticationTestFakes.InMemorySessionRepository();
    private FindSessionsService service;

    @BeforeEach
    void setUp() {
        users.add(activeAccount(42, "person@example.com"));
        users.add(activeAccount(43, "other@example.com"));
        for (int i = 1; i <= 100; i++) {
            // Several sessions share an issue time so the sessionId tie-break is exercised.
            sessions.seed(AuthenticationTestFakes.session(sessionId(i), 42, NOW.plusSeconds(i / 10)), "refresh-" + i);
        }
        sessions.seed(AuthenticationTestFakes.session(sessionId(900), 43, NOW), "foreign-1");
        sessions.get(sessionId(5)).revoke(NOW.plusSeconds(500));
        service = new FindSessionsService(users, new AuthenticationTestFakes.InMemorySessionViews(sessions));
    }

    @Test
    void lists_only_owned_retained_sessions_in_deterministic_order() {
        List<FindSessionsQuery.SessionView> views = service.handle(new FindSessionsQuery.Command(42));

        assertEquals(100, views.size());
        for (int i = 1; i < views.size(); i++) {
            FindSessionsQuery.SessionView previous = views.get(i - 1);
            FindSessionsQuery.SessionView current = views.get(i);
            assertTrue(previous.issuedAt().isAfter(current.issuedAt())
                    || (previous.issuedAt().equals(current.issuedAt())
                            && previous.sessionId().compareTo(current.sessionId()) < 0));
        }
        assertTrue(views.stream().allMatch(view -> view.userId() == 42));
        assertTrue(views.stream().anyMatch(view -> view.revokedAt() != null));
    }

    @Test
    void view_exposes_no_refresh_credential_or_digest_component() {
        List<String> components = java.util.Arrays.stream(FindSessionsQuery.SessionView.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(
                List.of("sessionId", "userId", "deviceInfo", "issuedAt", "expiresAt", "lastUsedAt", "revokedAt"),
                components);
    }

    @Test
    void inaccessible_account_cannot_list_sessions() {
        users.findById(new com.minewaku.chatter.identityaccess.user.internal.model.UserId(42))
                .orElseThrow()
                .suspend();

        assertThrows(AuthenticationFailedException.class, () -> service.handle(new FindSessionsQuery.Command(42)));
    }

    private static String sessionId(int n) {
        return String.format("0190f3a0-0000-7000-8000-%012d", n);
    }
}
