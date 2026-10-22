package com.minewaku.chatter.identityaccess.user.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationIssuer;
import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationTestFakes;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.throttle.InMemoryAttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.TestPolicies;
import com.minewaku.chatter.identityaccess.user.api.command.ConfirmEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.RequestEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.application.command.ConfirmEmailChangeService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.RequestEmailChangeService;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailChangeApplicationServiceTest {

    private static final String PASSWORD = "Password1!";
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private UserAccountTestFakes.DeterministicPasswordHasher hasher;
    private UserAccountTestFakes.InMemoryRepository accounts;
    private EmailVerificationTestFakes.Repository pending;
    private UserAccountTestFakes.RecordingEmailChangeNotifier notifier;
    private MutableClock clock;
    private InMemoryAttemptLimiter limiter;
    private RequestEmailChangeService request;
    private ConfirmEmailChangeService confirm;

    @BeforeEach
    void setUp() {
        hasher = new UserAccountTestFakes.DeterministicPasswordHasher();
        accounts = new UserAccountTestFakes.InMemoryRepository();
        pending = new EmailVerificationTestFakes.Repository();
        notifier = new UserAccountTestFakes.RecordingEmailChangeNotifier();
        clock = new MutableClock(START);
        limiter = new InMemoryAttemptLimiter();
        request = new RequestEmailChangeService(
                accounts,
                hasher,
                new EmailVerificationIssuer(clock, new UserAccountTestFakes.SequentialTokenGenerator(), pending),
                notifier,
                new ReauthenticationGuard(limiter, TestPolicies.defaults()),
                limiter,
                TestPolicies.defaults());
        confirm = new ConfirmEmailChangeService(pending, accounts, clock);
        accounts.add(account(42, "person@example.com", "person_42", AccountStatus.ACTIVE));
    }

    private UserAccount account(long id, String email, String username, AccountStatus status) {
        return UserAccount.reconstitute(
                new UserId(id),
                new Email(email),
                new Username(username),
                new Birthday(LocalDate.of(1990, 1, 1)),
                status,
                null,
                hasher.hash(new PlainPassword(PASSWORD)),
                START,
                START,
                0L);
    }

    private RequestEmailChangeUseCase.Result ask(String newEmail) {
        return request.handle(new RequestEmailChangeUseCase.Command(42, PASSWORD, newEmail));
    }

    private ConfirmEmailChangeUseCase.Result confirmToken(String token) {
        return confirm.handle(new ConfirmEmailChangeUseCase.Command(token));
    }

    private String latestToken() {
        return notifier.deliveries.getLast().token();
    }

    private String emailOf(long id) {
        return accounts.findById(new UserId(id)).orElseThrow().email().value();
    }

    @Test
    void five_wrong_passwords_block_the_sixth_request_even_with_the_right_password_and_send_nothing() {
        for (int i = 0; i < 5; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> request.handle(
                            new RequestEmailChangeUseCase.Command(42, "Wrong-password1", "new@example.com")));
        }

        assertThrows(TooManyAttemptsException.class, () -> ask("new@example.com"));

        assertTrue(notifier.deliveries.isEmpty());
        assertTrue(pending.records.isEmpty());
    }

    @Test
    void a_taken_address_after_the_password_was_accepted_does_not_keep_the_count() {
        accounts.add(account(43, "taken@example.com", "person_43", AccountStatus.ACTIVE));
        for (int i = 0; i < 4; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> request.handle(
                            new RequestEmailChangeUseCase.Command(42, "Wrong-password1", "new@example.com")));
        }

        assertThrows(DuplicateUserAccountException.class, () -> ask("taken@example.com"));

        for (int i = 0; i < 5; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> request.handle(
                            new RequestEmailChangeUseCase.Command(42, "Wrong-password1", "new@example.com")));
        }
    }

    @Test
    void a_mail_failure_propagates_leaves_the_email_unchanged_and_the_unsent_token_unusable() {
        notifier.failure = new MailDeliveryException();

        assertThrows(MailDeliveryException.class, () -> ask("new@example.com"));

        assertEquals("person@example.com", emailOf(42));
        assertTrue(notifier.deliveries.isEmpty());
        // nothing the person could have been sent confirms the change
        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken("a-token-nobody-received"));
        assertEquals("person@example.com", emailOf(42));
    }

    @Test
    void the_fourth_request_in_the_hour_is_refused_and_issues_and_mails_nothing() {
        for (int i = 0; i < 3; i++) {
            assertTrue(ask("new" + i + "@example.com").requested());
        }
        int mailed = notifier.deliveries.size();

        assertThrows(TooManyAttemptsException.class, () -> ask("another@example.com"));

        assertEquals(mailed, notifier.deliveries.size());
        assertEquals(
                new Email("new2@example.com"),
                pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42)
                        .orElseThrow()
                        .email());
    }

    @Test
    void a_wrong_password_an_unchanged_address_or_a_taken_address_do_not_use_up_the_allowance() {
        accounts.add(account(43, "taken@example.com", "person_43", AccountStatus.ACTIVE));
        assertThrows(
                InvalidCredentialsException.class,
                () -> request.handle(new RequestEmailChangeUseCase.Command(42, "Wrong-password1", "new@example.com")));
        assertFalse(ask("person@example.com").requested());
        assertThrows(DuplicateUserAccountException.class, () -> ask("taken@example.com"));

        for (int i = 0; i < 3; i++) {
            assertTrue(ask("new" + i + "@example.com").requested());
        }
        assertThrows(TooManyAttemptsException.class, () -> ask("another@example.com"));
    }

    @Test
    void a_new_window_allows_requests_again() {
        for (int i = 0; i < 3; i++) {
            ask("new" + i + "@example.com");
        }
        limiter.expire();

        assertTrue(ask("later@example.com").requested());
    }

    @Test
    void a_request_stores_one_pending_change_and_mails_only_the_new_address() {
        RequestEmailChangeUseCase.Result result = ask("New@Example.com");

        assertTrue(result.requested());
        assertEquals("person@example.com", emailOf(42));
        assertEquals(1, notifier.deliveries.size());
        UserAccountTestFakes.RecordingEmailChangeNotifier.Delivery delivery = notifier.deliveries.getFirst();
        assertEquals(new Email("new@example.com"), delivery.email());
        assertEquals(START.plus(Duration.ofHours(24)), delivery.expiresAt());
        assertEquals(
                new Email("new@example.com"),
                pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42)
                        .orElseThrow()
                        .email());
        assertEquals(0, accounts.saveCalls);
    }

    @Test
    void the_same_email_after_normalization_stores_nothing_and_sends_nothing() {
        RequestEmailChangeUseCase.Result result = ask(" PERSON@example.com ");

        assertFalse(result.requested());
        assertTrue(pending.records.isEmpty());
        assertTrue(notifier.deliveries.isEmpty());
    }

    @Test
    void an_email_held_by_another_account_is_a_conflict_and_stores_and_sends_nothing() {
        accounts.add(account(7, "taken@example.com", "other_7", AccountStatus.ACTIVE));

        assertThrows(DuplicateUserAccountException.class, () -> ask("Taken@Example.com"));

        assertTrue(pending.records.isEmpty());
        assertTrue(notifier.deliveries.isEmpty());
    }

    @Test
    void a_wrong_password_is_refused_before_anything_about_the_address_is_learned() {
        accounts.add(account(7, "taken@example.com", "other_7", AccountStatus.ACTIVE));
        ask("first@example.com");
        String firstToken = latestToken();

        assertThrows(
                InvalidCredentialsException.class,
                () -> request.handle(
                        new RequestEmailChangeUseCase.Command(42, "WrongPassword3#", "taken@example.com")));
        assertThrows(
                InvalidCredentialsException.class,
                () -> request.handle(new RequestEmailChangeUseCase.Command(42, "wrong", "second@example.com")));

        assertEquals(1, notifier.deliveries.size());
        assertTrue(pending.findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, firstToken)
                .isPresent());
        assertTrue(pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42).isPresent());
    }

    @Test
    void a_second_request_replaces_the_first_and_the_first_token_stops_working() {
        ask("first@example.com");
        String firstToken = latestToken();
        ask("second@example.com");
        String secondToken = latestToken();

        assertEquals(1, pending.records.size());
        assertTrue(pending.findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, firstToken)
                .isEmpty());
        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken(firstToken));
        assertInstanceOf(ConfirmEmailChangeUseCase.Confirmed.class, confirmToken(secondToken));
        assertEquals("second@example.com", emailOf(42));
    }

    @Test
    void a_failed_request_never_destroys_an_existing_pending_change() {
        ask("first@example.com");
        String firstToken = latestToken();
        accounts.add(account(7, "taken@example.com", "other_7", AccountStatus.ACTIVE));

        assertThrows(DuplicateUserAccountException.class, () -> ask("taken@example.com"));

        assertTrue(pending.findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, firstToken)
                .isPresent());
    }

    @Test
    void a_request_from_an_account_that_is_not_active_is_refused() {
        accounts.add(account(8, "sus@example.com", "sus_8", AccountStatus.SUSPENDED));

        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> request.handle(new RequestEmailChangeUseCase.Command(8, PASSWORD, "new@example.com")));

        assertTrue(notifier.deliveries.isEmpty());
    }

    @Test
    void confirming_the_token_replaces_the_email_and_keeps_the_record_as_confirmed() {
        ask("new@example.com");
        String token = latestToken();

        ConfirmEmailChangeUseCase.Result result = confirmToken(token);

        assertEquals(new ConfirmEmailChangeUseCase.Confirmed(42), result);
        assertEquals("new@example.com", emailOf(42));
        assertTrue(pending.findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, token)
                .orElseThrow()
                .isVerified());
        assertEquals(1, accounts.saveCalls);
    }

    @Test
    void confirming_again_is_reported_as_already_confirmed_and_changes_nothing_more() {
        ask("new@example.com");
        String token = latestToken();
        confirmToken(token);

        ConfirmEmailChangeUseCase.Result again = confirmToken(token);

        assertInstanceOf(ConfirmEmailChangeUseCase.AlreadyConfirmed.class, again);
        assertEquals(1, accounts.saveCalls);
    }

    @Test
    void blank_unknown_and_expired_tokens_are_invalid_and_change_nothing() {
        ask("new@example.com");
        String token = latestToken();

        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken(null));
        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken("  "));
        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken("unknown"));
        clock.advance(Duration.ofHours(24));
        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken(token));

        assertEquals("person@example.com", emailOf(42));
    }

    @Test
    void an_address_taken_in_the_meantime_is_a_conflict_and_the_pending_change_is_dropped() {
        ask("new@example.com");
        String token = latestToken();
        accounts.add(account(7, "new@example.com", "other_7", AccountStatus.ACTIVE));

        assertThrows(DuplicateUserAccountException.class, () -> confirmToken(token));

        assertEquals("person@example.com", emailOf(42));
        assertTrue(pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42).isEmpty());
        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken(token));
    }

    @Test
    void a_uniqueness_race_at_save_time_also_drops_the_pending_change() {
        ask("new@example.com");
        String token = latestToken();
        accounts.duplicateOnNextSave = true;
        accounts.accountToExposeAfterDuplicate = account(7, "new@example.com", "other_7", AccountStatus.ACTIVE);

        assertThrows(DuplicateUserAccountException.class, () -> confirmToken(token));

        assertTrue(pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42).isEmpty());
    }

    @Test
    void an_account_that_is_no_longer_active_gets_the_generic_answer() {
        ask("new@example.com");
        String token = latestToken();
        accounts.add(account(42, "person@example.com", "person_42", AccountStatus.SUSPENDED));

        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken(token));

        assertEquals("person@example.com", emailOf(42));
    }

    @Test
    void a_retry_after_the_email_was_applied_but_not_recorded_settles_as_already_confirmed() {
        ask("new@example.com");
        String token = latestToken();
        accounts.findById(new UserId(42)).orElseThrow().applyVerifiedEmail(new Email("new@example.com"));

        assertInstanceOf(ConfirmEmailChangeUseCase.AlreadyConfirmed.class, confirmToken(token));

        assertTrue(pending.findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, token)
                .orElseThrow()
                .isVerified());
    }

    @Test
    void a_registration_proof_cannot_confirm_an_email_change() {
        EmailVerification registration = EmailVerification.issue(
                VerificationPurpose.REGISTRATION,
                "registration-token",
                new UserId(42),
                new Email("new@example.com"),
                clock,
                Duration.ofHours(24));
        pending.save(registration);

        assertInstanceOf(ConfirmEmailChangeUseCase.InvalidOrExpired.class, confirmToken("registration-token"));

        assertEquals("person@example.com", emailOf(42));
        assertFalse(registration.isVerified());
    }

    @Test
    void the_token_lifetime_is_the_registration_lifetime() {
        assertEquals(
                com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationIssuer
                        .LIFETIME,
                Duration.ofHours(24));
        ask("new@example.com");

        var change =
                pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42).orElseThrow();
        assertEquals(Duration.ofHours(24), Duration.between(change.createdAt(), change.expiresAt()));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
