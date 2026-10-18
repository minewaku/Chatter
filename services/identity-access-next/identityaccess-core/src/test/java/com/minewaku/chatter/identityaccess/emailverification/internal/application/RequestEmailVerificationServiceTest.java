package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RequestEmailVerificationServiceTest {
    @Test
    void accepts_pending_and_ineligible_emails_uniformly_while_issuing_only_for_pending() {
        var tokens = new EmailVerificationTestFakes.Generator();
        var verificationRepository = new EmailVerificationTestFakes.Repository();
        var notifier = new EmailVerificationTestFakes.Notifier();
        var coordinator = new EmailVerificationIssuanceCoordinator(
                new EmailVerificationIssuer(
                        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                        tokens,
                        verificationRepository),
                notifier);
        var accounts = new Accounts();
        accounts.account = account(AccountStatus.PENDING_VERIFICATION);
        var service = new RequestEmailVerificationService(accounts, coordinator);

        assertEquals(
                new RequestEmailVerificationUseCase.Accepted(),
                service.handle(new RequestEmailVerificationUseCase.Command(" PERSON@Example.com ")));
        assertTrue(verificationRepository.records.containsKey("token-1"));
        assertEquals(
                new RequestEmailVerificationUseCase.Accepted(),
                service.handle(new RequestEmailVerificationUseCase.Command("person@example.com")));
        assertFalse(verificationRepository.records.containsKey("token-1"));
        assertTrue(verificationRepository.records.containsKey("token-2"));
        verificationRepository.records.get("token-2").verify(Instant.parse("2026-01-01T00:00:01Z"));
        assertEquals(
                new RequestEmailVerificationUseCase.Accepted(),
                service.handle(new RequestEmailVerificationUseCase.Command("person@example.com")));
        assertEquals(
                new RequestEmailVerificationUseCase.Accepted(),
                service.handle(new RequestEmailVerificationUseCase.Command("absent@example.com")));
        accounts.account = account(AccountStatus.ACTIVE);
        assertEquals(
                new RequestEmailVerificationUseCase.Accepted(),
                service.handle(new RequestEmailVerificationUseCase.Command("person@example.com")));
        assertEquals(3, tokens.calls);
        assertEquals(3, notifier.deliveries.size());
        assertTrue(verificationRepository.records.containsKey("token-2"));
        assertTrue(verificationRepository.records.containsKey("token-3"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.handle(new RequestEmailVerificationUseCase.Command("bad")));
    }

    @Test
    void a_resend_leaves_an_email_change_proof_of_the_same_user_untouched() {
        var tokens = new EmailVerificationTestFakes.Generator();
        var verificationRepository = new EmailVerificationTestFakes.Repository();
        var coordinator = new EmailVerificationIssuanceCoordinator(
                new EmailVerificationIssuer(
                        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                        tokens,
                        verificationRepository),
                new EmailVerificationTestFakes.Notifier());
        var accounts = new Accounts();
        accounts.account = account(AccountStatus.PENDING_VERIFICATION);
        var service = new RequestEmailVerificationService(accounts, coordinator);
        verificationRepository.save(EmailVerification.issue(
                VerificationPurpose.EMAIL_CHANGE,
                "change-token",
                new UserId(1),
                new Email("new@example.com"),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofHours(24)));

        service.handle(new RequestEmailVerificationUseCase.Command("person@example.com"));
        service.handle(new RequestEmailVerificationUseCase.Command("person@example.com"));

        assertTrue(verificationRepository
                .outstandingFor(VerificationPurpose.EMAIL_CHANGE, 1)
                .isPresent());
        assertFalse(verificationRepository.records.containsKey("token-1"));
        assertTrue(verificationRepository.records.containsKey("token-2"));
    }

    private static UserAccount account(AccountStatus status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return UserAccount.reconstitute(
                new UserId(1),
                new Email("person@example.com"),
                new Username("person_1"),
                new Birthday(LocalDate.of(1990, 1, 1)),
                status,
                null,
                new PasswordHash("test", "x", new byte[] {1}),
                now,
                now,
                0L);
    }

    private static final class Accounts implements UserAccountRepository {
        UserAccount account;

        @Override
        public Optional<UserAccount> findById(UserId id) {
            return Optional.ofNullable(account);
        }

        @Override
        public Optional<UserAccount> findByEmail(Email email) {
            return account != null && account.email().equals(email) ? Optional.of(account) : Optional.empty();
        }

        @Override
        public Optional<UserAccount> findByUsername(Username username) {
            return Optional.empty();
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            account = userAccount;
            return account;
        }
    }
}
