package com.minewaku.chatter.identityaccess.authentication.internal.application;

import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.AccessTokenIssuer;
import com.minewaku.chatter.identityaccess.authentication.internal.port.RefreshTokenGenerator;
import com.minewaku.chatter.identityaccess.authentication.internal.port.query.AuthenticationSessionViewRepository;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AuthenticationTestFakes {

    public static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
    public static final String PASSWORD = "Password1!";

    private AuthenticationTestFakes() {}

    public static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    public static UserAccount activeAccount(long id, String email) {
        UserAccount account = UserAccount.register(
                new UserId(id),
                new Email(email),
                new Username("person_" + id),
                new Birthday(LocalDate.of(1990, 1, 1)),
                new PasswordHasherFake().hash(new PlainPassword(PASSWORD)));
        account.activateAfterVerification();
        return account;
    }

    public static AuthenticationSession session(String sessionId, long userId, Instant issuedAt) {
        return AuthenticationSession.issue(
                sessionId, new UserId(userId), "Test device", issuedAt, java.time.Duration.ofDays(30));
    }

    public static final class PasswordHasherFake implements PasswordHasher {
        @Override
        public PasswordHash hash(PlainPassword password) {
            return new PasswordHash("test", String.valueOf(password.characters()), new byte[] {1});
        }

        @Override
        public boolean matches(PlainPassword password, PasswordHash passwordHash) {
            return passwordHash.hash().equals(String.valueOf(password.characters()));
        }
    }

    public static final class InMemoryUserAccounts implements UserAccountRepository {
        private final Map<UserId, UserAccount> accounts = new LinkedHashMap<>();

        public void add(UserAccount account) {
            accounts.put(account.id(), account);
        }

        @Override
        public Optional<UserAccount> findById(UserId userId) {
            return Optional.ofNullable(accounts.get(userId));
        }

        @Override
        public Optional<UserAccount> findByEmail(Email email) {
            return accounts.values().stream()
                    .filter(account -> account.email().equals(email))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findByUsername(Username username) {
            return accounts.values().stream()
                    .filter(account -> account.username().equals(username))
                    .findFirst();
        }

        @Override
        public UserAccount save(UserAccount account) {
            accounts.put(account.id(), account);
            return account;
        }
    }

    /** Stores sessions and their raw-token association; enforces owner filters and conditional writes. */
    public static final class InMemorySessionRepository implements AuthenticationSessionRepository {
        private final Map<String, AuthenticationSession> sessions = new LinkedHashMap<>();
        private final Map<String, String> sessionIdByToken = new HashMap<>();
        public int createCalls;
        public int revokeAllCalls;
        public boolean failOnCreate;
        public boolean failOnRevokeAll;

        public void seed(AuthenticationSession session, String rawRefreshToken) {
            sessions.put(session.sessionId(), session);
            sessionIdByToken.put(rawRefreshToken, session.sessionId());
        }

        public AuthenticationSession get(String sessionId) {
            return sessions.get(sessionId);
        }

        public List<AuthenticationSession> sessionsOf(UserId userId) {
            return sessions.values().stream()
                    .filter(session -> session.userId().equals(userId))
                    .toList();
        }

        @Override
        public void create(AuthenticationSession session, String rawRefreshToken) {
            if (failOnCreate) {
                throw new IllegalStateException("session store unavailable");
            }
            if (sessions.containsKey(session.sessionId())) {
                throw new IllegalStateException("duplicate session id");
            }
            seed(session, rawRefreshToken);
            createCalls++;
        }

        @Override
        public Optional<AuthenticationSession> findByPresentedToken(String rawRefreshToken) {
            return Optional.ofNullable(sessionIdByToken.get(rawRefreshToken)).map(sessions::get);
        }

        @Override
        public Optional<AuthenticationSession> findBySessionId(String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public boolean touchIfUsable(String sessionId, Instant now) {
            AuthenticationSession session = sessions.get(sessionId);
            if (session == null || !session.isUsable(now)) {
                return false;
            }
            session.markUsed(now);
            return true;
        }

        @Override
        public boolean revokeBySessionId(UserId userId, String sessionId, Instant now) {
            AuthenticationSession session = sessions.get(sessionId);
            if (session == null || !session.userId().equals(userId)) {
                return false;
            }
            return session.revoke(now);
        }

        @Override
        public int revokeOtherSessions(UserId userId, String currentSessionId, Instant now) {
            int changed = 0;
            for (AuthenticationSession session : sessionsOf(userId)) {
                if (!session.sessionId().equals(currentSessionId) && session.revoke(now)) {
                    changed++;
                }
            }
            return changed;
        }

        @Override
        public int revokeAllByUserId(UserId userId, Instant now) {
            if (failOnRevokeAll) {
                throw new IllegalStateException("revocation store unavailable");
            }
            revokeAllCalls++;
            int changed = 0;
            for (AuthenticationSession session : sessionsOf(userId)) {
                if (session.revoke(now)) {
                    changed++;
                }
            }
            return changed;
        }
    }

    public static final class InMemorySessionViews implements AuthenticationSessionViewRepository {
        private final InMemorySessionRepository sessions;

        public InMemorySessionViews(InMemorySessionRepository sessions) {
            this.sessions = sessions;
        }

        @Override
        public List<FindSessionsQuery.SessionView> findByUserId(UserId userId) {
            List<AuthenticationSession> owned = new ArrayList<>(sessions.sessionsOf(userId));
            owned.sort(Comparator.comparing(AuthenticationSession::issuedAt)
                    .reversed()
                    .thenComparing(AuthenticationSession::sessionId));
            return owned.stream()
                    .map(s -> new FindSessionsQuery.SessionView(
                            s.sessionId(),
                            s.userId().value(),
                            s.deviceInfo(),
                            s.issuedAt(),
                            s.expiresAt(),
                            s.lastUsedAt(),
                            s.revokedAt()))
                    .toList();
        }
    }

    public static final class SequenceSessionIds implements UniqueStringIdGenerator {
        private long next = 1;

        @Override
        public String nextId() {
            return String.format("0190f3a0-0000-7000-8000-%012d", next++);
        }
    }

    public static final class SequenceRefreshTokens implements RefreshTokenGenerator {
        private int next = 1;

        @Override
        public String generate() {
            return "refresh-" + next++;
        }
    }

    public static final class RecordingAccessTokenIssuer implements AccessTokenIssuer {
        public int issueCalls;
        public boolean failOnIssue;
        private int next = 1;

        @Override
        public String issue(UserId userId, Email email) {
            if (failOnIssue) {
                throw new IllegalStateException("token issuer unavailable");
            }
            issueCalls++;
            return "access-" + next++;
        }
    }
}
