package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.minewaku.chatter.identityaccess.emailverification.command.VerifyEmailUseCase;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VerifyEmailServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final EmailVerificationTestFakes.Repository verifications = new EmailVerificationTestFakes.Repository();
    private final Accounts accounts = new Accounts();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final VerifyEmailService service = new VerifyEmailService(verifications, accounts, clock);

    @Test
    void consumes_matching_pending_account_once_and_replays_as_already_verified() {
        UserAccount account = account(AccountStatus.PENDING_VERIFICATION, "person@example.com");
        accounts.add(account);
        verifications.records.put("current", verification("current", account.id(), "person@example.com", null));

        assertEquals(new VerifyEmailUseCase.Verified(7), service.handle(new VerifyEmailUseCase.Command("current")));
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertInstanceOf(
                VerifyEmailUseCase.AlreadyVerified.class, service.handle(new VerifyEmailUseCase.Command("current")));
    }

    @Test
    void rejects_blank_missing_expired_wrong_status_missing_account_and_email_mismatch() {
        assertInstanceOf(
                VerifyEmailUseCase.InvalidOrExpired.class, service.handle(new VerifyEmailUseCase.Command(" ")));
        assertInstanceOf(
                VerifyEmailUseCase.InvalidOrExpired.class, service.handle(new VerifyEmailUseCase.Command("missing")));
        UserAccount pending = account(AccountStatus.PENDING_VERIFICATION, "person@example.com");
        accounts.add(pending);
        verifications.records.put("expired", verification("expired", pending.id(), "person@example.com", null, NOW));
        verifications.records.put("mismatch", verification("mismatch", pending.id(), "other@example.com", null));
        assertInstanceOf(
                VerifyEmailUseCase.InvalidOrExpired.class, service.handle(new VerifyEmailUseCase.Command("expired")));
        assertInstanceOf(
                VerifyEmailUseCase.InvalidOrExpired.class, service.handle(new VerifyEmailUseCase.Command("mismatch")));
        UserAccount active = account(AccountStatus.ACTIVE, "active@example.com");
        accounts.add(active);
        verifications.records.put("active", verification("active", active.id(), "active@example.com", null));
        assertInstanceOf(
                VerifyEmailUseCase.InvalidOrExpired.class, service.handle(new VerifyEmailUseCase.Command("active")));
        verifications.records.put("orphan", verification("orphan", new UserId(99), "orphan@example.com", null));
        assertInstanceOf(
                VerifyEmailUseCase.InvalidOrExpired.class, service.handle(new VerifyEmailUseCase.Command("orphan")));
    }

    @Test
    void propagates_token_and_account_save_failures() {
        UserAccount account = account(AccountStatus.PENDING_VERIFICATION, "person@example.com");
        accounts.add(account);
        verifications.records.put("current", verification("current", account.id(), "person@example.com", null));
        verifications.failure = new IllegalStateException("save failed");
        assertThrows(IllegalStateException.class, () -> service.handle(new VerifyEmailUseCase.Command("current")));
    }

    private static EmailVerification verification(String token, UserId id, String email, Instant used) {
        return verification(token, id, email, used, NOW.plusSeconds(3600));
    }

    private static EmailVerification verification(
            String token, UserId id, String email, Instant used, Instant expires) {
        return EmailVerification.reconstitute(token, id, new Email(email), NOW.minusSeconds(10), expires, used);
    }

    private static UserAccount account(AccountStatus status, String email) {
        Instant timestamp = NOW;
        return UserAccount.reconstitute(
                new UserId(email.startsWith("person") ? 7 : email.startsWith("active") ? 8 : 9),
                new Email(email),
                new Username("person_" + (email.startsWith("person") ? 7 : 8)),
                new Birthday(LocalDate.of(1990, 1, 1)),
                status,
                null,
                new PasswordHash("test", "x", new byte[] {1}),
                timestamp,
                timestamp,
                0L);
    }

    private static final class Accounts implements UserAccountRepository {
        final Map<UserId, UserAccount> byId = new HashMap<>();

        void add(UserAccount account) {
            byId.put(account.id(), account);
        }

        @Override
        public Optional<UserAccount> findById(UserId id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<UserAccount> findByEmail(Email email) {
            return byId.values().stream().filter(a -> a.email().equals(email)).findFirst();
        }

        @Override
        public Optional<UserAccount> findByUsername(Username username) {
            return Optional.empty();
        }

        @Override
        public UserAccount save(UserAccount account) {
            byId.put(account.id(), account);
            return account;
        }
    }
}
