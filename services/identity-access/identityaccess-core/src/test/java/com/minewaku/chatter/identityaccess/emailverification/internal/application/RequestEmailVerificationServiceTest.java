package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.throttle.InMemoryAttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.TestPolicies;
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
import java.util.List;
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
        var service = new RequestEmailVerificationService(
                accounts, coordinator, new InMemoryAttemptLimiter(), TestPolicies.generous());

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
        var service = new RequestEmailVerificationService(
                accounts, coordinator, new InMemoryAttemptLimiter(), TestPolicies.generous());
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

    /** A service wired with the documented default limits, so the throttle itself can be exercised. */
    private static final class Throttled {
        final EmailVerificationTestFakes.Notifier notifier = new EmailVerificationTestFakes.Notifier();
        final InMemoryAttemptLimiter limiter = new InMemoryAttemptLimiter();
        final Accounts accounts = new Accounts();
        final RequestEmailVerificationService service;

        Throttled() {
            var tokens = new EmailVerificationTestFakes.Generator();
            for (int i = 4; i <= 12; i++) {
                tokens.values.add("token-" + i);
            }
            accounts.account = account(AccountStatus.PENDING_VERIFICATION);
            service = new RequestEmailVerificationService(
                    accounts,
                    new EmailVerificationIssuanceCoordinator(
                            new EmailVerificationIssuer(
                                    Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                                    tokens,
                                    new EmailVerificationTestFakes.Repository()),
                            notifier),
                    limiter,
                    TestPolicies.defaults());
        }

        void resend(String email) {
            service.handle(new RequestEmailVerificationUseCase.Command(email));
        }
    }

    @Test
    void a_second_request_inside_the_gap_is_refused_and_sends_nothing() {
        var t = new Throttled();
        t.resend("person@example.com");

        TooManyAttemptsException refused =
                assertThrows(TooManyAttemptsException.class, () -> t.resend("person@example.com"));

        assertEquals(1, t.notifier.deliveries.size());
        assertTrue(refused.retryAfter().compareTo(Duration.ZERO) > 0);
        assertTrue(refused.retryAfter().compareTo(Duration.ofMinutes(1)) <= 0);
    }

    @Test
    void after_the_gap_the_next_request_is_accepted_and_the_sixth_in_the_hour_is_refused() {
        var t = new Throttled();
        var gap = TestPolicies.defaults().resendGap();
        for (int i = 0; i < 5; i++) {
            t.resend("person@example.com");
            t.limiter.expireOnly(gap, "person@example.com");
        }

        assertThrows(TooManyAttemptsException.class, () -> t.resend("person@example.com"));
        assertEquals(5, t.notifier.deliveries.size());
    }

    @Test
    void a_request_refused_by_the_gap_does_not_use_up_the_hourly_allowance() {
        var t = new Throttled();
        t.resend("person@example.com");
        for (int i = 0; i < 10; i++) {
            assertThrows(TooManyAttemptsException.class, () -> t.resend("person@example.com"));
        }

        assertEquals(1, t.limiter.count(TestPolicies.defaults().resendHourly(), "person@example.com"));
    }

    @Test
    void an_address_with_no_account_is_throttled_exactly_like_a_real_one() {
        var t = new Throttled();
        t.resend("absent@example.com");

        TooManyAttemptsException refused =
                assertThrows(TooManyAttemptsException.class, () -> t.resend("absent@example.com"));

        assertTrue(refused.retryAfter().compareTo(Duration.ZERO) > 0);
        assertTrue(t.notifier.deliveries.isEmpty());
    }

    @Test
    void spelling_and_case_do_not_give_an_address_a_second_allowance() {
        var t = new Throttled();
        t.resend("person@example.com");

        assertThrows(TooManyAttemptsException.class, () -> t.resend("  PERSON@Example.com "));
    }

    @Test
    void a_malformed_address_is_not_counted() {
        var t = new Throttled();

        assertThrows(IllegalArgumentException.class, () -> t.resend("bad"));
        t.resend("person@example.com");

        assertEquals(1, t.notifier.deliveries.size());
    }

    @Test
    void a_mail_failure_is_hidden_and_the_resend_is_still_accepted() {
        var t = new Throttled();
        t.notifier.failure = new MailDeliveryException();

        assertEquals(
                new RequestEmailVerificationUseCase.Accepted(),
                t.service.handle(new RequestEmailVerificationUseCase.Command("person@example.com")));
        assertEquals(List.of("notify"), t.notifier.calls);
    }

    @Test
    void an_unavailable_limiter_stops_the_request() {
        var t = new Throttled();
        t.limiter.setUnavailable(true);

        assertThrows(DependencyUnavailableException.class, () -> t.resend("person@example.com"));
        assertTrue(t.notifier.deliveries.isEmpty());
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
