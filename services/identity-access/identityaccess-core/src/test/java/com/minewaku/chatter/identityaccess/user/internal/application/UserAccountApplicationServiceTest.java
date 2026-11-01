package com.minewaku.chatter.identityaccess.user.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationSessionRevocationCoordinator;
import com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationTestFakes;
import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationIssuanceCoordinator;
import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationIssuer;
import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationTestFakes;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.throttle.InMemoryAttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.TestPolicies;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ChangeUsernameUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.LockUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ReinstateUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SuspendUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.UnlockUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.application.command.ChangePasswordService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.ChangeUsernameService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.LockUserAccountService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.RegisterUserService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.ReinstateUserAccountService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.SoftDeleteUserAccountService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.SuspendUserAccountService;
import com.minewaku.chatter.identityaccess.user.internal.application.command.UnlockUserAccountService;
import com.minewaku.chatter.identityaccess.user.internal.application.query.FindUserByEmailService;
import com.minewaku.chatter.identityaccess.user.internal.application.query.FindUserByIdService;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.UserAccountEventPublisher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class UserAccountApplicationServiceTest {

    /** A fresh guard each time, so counts never leak between tests. */
    private static ReauthenticationGuard guard() {
        return new ReauthenticationGuard(new InMemoryAttemptLimiter(), TestPolicies.defaults());
    }

    private static final String CURRENT_PASSWORD = "Password1!";
    /** Shared by call sites that do not assert on announcements; tests that do build their own. */
    private static final RecordingPublisher PUBLISHER = new RecordingPublisher();
    private static final EmailVerificationTestFakes.Repository NO_PENDING_CHANGES =
            new EmailVerificationTestFakes.Repository();
    private static final AuthenticationSessionRevocationCoordinator SESSION_REVOCATION =
            new AuthenticationSessionRevocationCoordinator(
                    new AuthenticationTestFakes.InMemorySessionRepository(),
                    AuthenticationTestFakes.clockAt(AuthenticationTestFakes.NOW));
    private static final ExistingAccountCommand LOCK =
            (repository, hasher, userId) -> new LockUserAccountService(repository)
                    .handle(new LockUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand UNLOCK =
            (repository, hasher, userId) -> new UnlockUserAccountService(repository)
                    .handle(new UnlockUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand SUSPEND =
            (repository, hasher, userId) -> new SuspendUserAccountService(repository, SESSION_REVOCATION)
                    .handle(new SuspendUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand REINSTATE =
            (repository, hasher, userId) -> new ReinstateUserAccountService(repository)
                    .handle(new ReinstateUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand SOFT_DELETE = (repository, hasher, userId) ->
            new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), PUBLISHER)
                    .handle(new SoftDeleteUserAccountUseCase.Command(userId, CURRENT_PASSWORD))
                    .changed();
    private static final ExistingAccountCommand CHANGE_PASSWORD =
            (repository, hasher, userId) -> new ChangePasswordService(repository, hasher, SESSION_REVOCATION, guard())
                    .handle(new ChangePasswordUseCase.Command(userId, CURRENT_PASSWORD, "NewPassword2@"))
                    .changed();
    private static final ExistingAccountCommand KEEP_PASSWORD =
            (repository, hasher, userId) -> new ChangePasswordService(repository, hasher, SESSION_REVOCATION, guard())
                    .handle(new ChangePasswordUseCase.Command(userId, CURRENT_PASSWORD, CURRENT_PASSWORD))
                    .changed();
    private static final ExistingAccountCommand CHANGE_USERNAME =
            (repository, hasher, userId) -> new ChangeUsernameService(repository)
                    .handle(new ChangeUsernameUseCase.Command(userId, "new_person"))
                    .changed();
    private static final ExistingAccountCommand KEEP_USERNAME =
            (repository, hasher, userId) -> new ChangeUsernameService(repository)
                    .handle(new ChangeUsernameUseCase.Command(userId, "person_42"))
                    .changed();

    @Test
    void change_password_hashes_and_saves_once_only_when_value_changes() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount account = activeAccount(hasher);
        repository.add(account);
        ChangePasswordService service = new ChangePasswordService(repository, hasher, SESSION_REVOCATION, guard());

        ChangePasswordUseCase.Result changed =
                service.handle(new ChangePasswordUseCase.Command(42, CURRENT_PASSWORD, "NewPassword2@"));
        ChangePasswordUseCase.Result unchanged =
                service.handle(new ChangePasswordUseCase.Command(42, "NewPassword2@", "NewPassword2@"));

        assertTrue(changed.changed());
        assertFalse(unchanged.changed());
        assertEquals(1, repository.saveCalls);
        assertTrue(hasher.matches(new PlainPassword("NewPassword2@"), account.passwordHash()));
    }

    @Test
    void wrong_passwords_across_change_password_and_close_share_one_count_and_leave_the_account_untouched() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount account = activeAccount(hasher);
        repository.add(account);
        ReauthenticationGuard shared = guard();
        ChangePasswordService passwords = new ChangePasswordService(repository, hasher, SESSION_REVOCATION, shared);
        SoftDeleteUserAccountService closing =
                new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, shared, PUBLISHER);
        for (int i = 0; i < 3; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> passwords.handle(new ChangePasswordUseCase.Command(42, "Wrong-password1", "NewPassword2@")));
        }
        for (int i = 0; i < 2; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> closing.handle(new SoftDeleteUserAccountUseCase.Command(42, "Wrong-password1")));
        }

        assertThrows(
                TooManyAttemptsException.class,
                () -> closing.handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD)));
        assertThrows(
                TooManyAttemptsException.class,
                () -> passwords.handle(new ChangePasswordUseCase.Command(42, CURRENT_PASSWORD, "NewPassword2@")));

        assertEquals(0, repository.saveCalls);
        assertTrue(hasher.matches(new PlainPassword(CURRENT_PASSWORD), account.passwordHash()));
    }

    @Test
    void a_correct_password_clears_the_count_so_the_next_five_wrong_ones_are_allowed_again() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        ChangePasswordService passwords = new ChangePasswordService(repository, hasher, SESSION_REVOCATION, guard());
        for (int i = 0; i < 4; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> passwords.handle(new ChangePasswordUseCase.Command(42, "Wrong-password1", "NewPassword2@")));
        }

        assertTrue(passwords
                .handle(new ChangePasswordUseCase.Command(42, CURRENT_PASSWORD, "NewPassword2@"))
                .changed());

        for (int i = 0; i < 5; i++) {
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> passwords.handle(new ChangePasswordUseCase.Command(42, "Wrong-password1", "Other-pass3#")));
        }
        assertThrows(
                TooManyAttemptsException.class,
                () -> passwords.handle(new ChangePasswordUseCase.Command(42, "NewPassword2@", "Other-pass3#")));
    }

    @Test
    void change_username_saves_only_for_new_values() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        ChangeUsernameService usernameService = new ChangeUsernameService(repository);

        assertTrue(usernameService
                .handle(new ChangeUsernameUseCase.Command(42, "new_person"))
                .changed());
        assertFalse(usernameService
                .handle(new ChangeUsernameUseCase.Command(42, "new_person"))
                .changed());
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void a_username_held_by_another_account_surfaces_as_a_duplicate() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount account = activeAccount(hasher);
        repository.add(account);
        repository.duplicateOnNextSave = true;
        repository.accountToExposeAfterDuplicate = account;

        assertThrows(DuplicateUserAccountException.class, () -> new ChangeUsernameService(repository)
                .handle(new ChangeUsernameUseCase.Command(42, "taken_name")));
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void registration_creates_and_saves_one_pending_account_when_email_is_available() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        List<String> deliveries = new ArrayList<>();
        EmailVerificationIssuanceCoordinator issuance = new EmailVerificationIssuanceCoordinator(
                new EmailVerificationIssuer(
                        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                        () -> "registration-token",
                        new EmptyVerificationRepository()),
                (email, token, expiresAt) -> deliveries.add(token));

        RegisterUserUseCase.Result registered =
                new RegisterUserService(repository, ids, hasher, issuance).handle(registrationCommand());

        assertEquals(new RegisterUserUseCase.Registered(100, true), registered);
        assertEquals(1, repository.findByEmailCalls);
        assertEquals(1, ids.calls);
        assertEquals(1, repository.saveCalls);
        assertEquals(List.of("registration-token"), deliveries);
    }

    @Test
    void a_mail_failure_does_not_undo_the_registration_and_is_reported() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        EmailVerificationIssuanceCoordinator failingMail = new EmailVerificationIssuanceCoordinator(
                new EmailVerificationIssuer(
                        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                        () -> "registration-token",
                        new EmptyVerificationRepository()),
                (email, token, expiresAt) -> {
                    throw new MailDeliveryException();
                });

        RegisterUserUseCase.Result result =
                new RegisterUserService(repository, ids, hasher, failingMail).handle(registrationCommand());

        assertEquals(new RegisterUserUseCase.Registered(100, false), result);
        assertEquals(1, repository.saveCalls);
        assertEquals(
                AccountStatus.PENDING_VERIFICATION,
                repository.findById(new UserId(100)).orElseThrow().status());
    }

    @Test
    void a_failure_other_than_a_mail_failure_still_stops_the_registration() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        EmailVerificationIssuanceCoordinator broken = new EmailVerificationIssuanceCoordinator(
                new EmailVerificationIssuer(
                        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                        () -> "registration-token",
                        new EmptyVerificationRepository()),
                (email, token, expiresAt) -> {
                    throw new IllegalStateException("store down");
                });

        assertThrows(IllegalStateException.class, () -> new RegisterUserService(
                        new UserAccountTestFakes.InMemoryRepository(), ids, hasher, broken)
                .handle(registrationCommand()));
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void registration_classifies_every_existing_status_without_creating_or_saving(AccountStatus status) {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(account(status, hasher));

        RegisterUserUseCase.Result result =
                new RegisterUserService(repository, ids, hasher, issuance()).handle(registrationCommand());

        assertRegistrationClassification(status, result);
        assertEquals(1, repository.findByEmailCalls);
        assertEquals(0, ids.calls);
        assertEquals(0, repository.saveCalls);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void registration_retry_after_a_uniqueness_race_applies_the_same_classification(AccountStatus status) {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.duplicateOnNextSave = true;
        repository.accountToExposeAfterDuplicate = account(status, hasher);
        RegisterUserService service = new RegisterUserService(repository, ids, hasher, issuance());

        assertThrows(DuplicateUserAccountException.class, () -> service.handle(registrationCommand()));
        RegisterUserUseCase.Result result = service.handle(registrationCommand());

        assertRegistrationClassification(status, result);
        assertEquals(2, repository.findByEmailCalls);
        assertEquals(1, ids.calls);
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void state_command_services_save_only_actual_changes() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));

        assertTrue(new LockUserAccountService(repository)
                .handle(new LockUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new LockUserAccountService(repository)
                .handle(new LockUserAccountUseCase.Command(42))
                .changed());
        assertTrue(new UnlockUserAccountService(repository)
                .handle(new UnlockUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new UnlockUserAccountService(repository)
                .handle(new UnlockUserAccountUseCase.Command(42))
                .changed());
        assertTrue(new SuspendUserAccountService(repository, SESSION_REVOCATION)
                .handle(new SuspendUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new SuspendUserAccountService(repository, SESSION_REVOCATION)
                .handle(new SuspendUserAccountUseCase.Command(42))
                .changed());
        assertTrue(new ReinstateUserAccountService(repository)
                .handle(new ReinstateUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new ReinstateUserAccountService(repository)
                .handle(new ReinstateUserAccountUseCase.Command(42))
                .changed());
        assertTrue(new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), PUBLISHER)
                .handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD))
                .changed());
        assertThrows(InvalidCredentialsException.class, () -> new SoftDeleteUserAccountService(
                        repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), PUBLISHER)
                .handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD)));
        assertEquals(5, repository.saveCalls);
    }

    @Test
    void account_changes_revoke_all_sessions_only_when_they_actually_change_the_account() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        AuthenticationTestFakes.InMemorySessionRepository sessions =
                new AuthenticationTestFakes.InMemorySessionRepository();
        sessions.seed(
                AuthenticationTestFakes.session(
                        "0190f3a0-0000-7000-8000-000000000001", 42, AuthenticationTestFakes.NOW),
                "raw-1");
        AuthenticationSessionRevocationCoordinator revocation = new AuthenticationSessionRevocationCoordinator(
                sessions, AuthenticationTestFakes.clockAt(AuthenticationTestFakes.NOW));
        ChangePasswordService passwords = new ChangePasswordService(repository, hasher, revocation, guard());

        assertThrows(
                RuntimeException.class,
                () -> passwords.handle(new ChangePasswordUseCase.Command(42, "WrongPassword3#", "NewPassword2@")));
        assertFalse(passwords
                .handle(new ChangePasswordUseCase.Command(42, CURRENT_PASSWORD, CURRENT_PASSWORD))
                .changed());
        assertEquals(0, sessions.revokeAllCalls);

        assertTrue(passwords
                .handle(new ChangePasswordUseCase.Command(42, CURRENT_PASSWORD, "NewPassword2@"))
                .changed());
        assertEquals(1, sessions.revokeAllCalls);
        assertTrue(new SuspendUserAccountService(repository, revocation)
                .handle(new SuspendUserAccountUseCase.Command(42))
                .changed());
        assertEquals(2, sessions.revokeAllCalls);
        assertTrue(new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, revocation, guard(), PUBLISHER)
                .handle(new SoftDeleteUserAccountUseCase.Command(42, "NewPassword2@"))
                .changed());
        assertEquals(3, sessions.revokeAllCalls);
        assertEquals(
                AuthenticationTestFakes.NOW,
                sessions.get("0190f3a0-0000-7000-8000-000000000001").revokedAt());
    }

    @Test
    void close_with_a_wrong_password_is_refused_without_touching_the_account_or_any_session() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount account = activeAccount(hasher);
        repository.add(account);
        AuthenticationTestFakes.InMemorySessionRepository sessions =
                new AuthenticationTestFakes.InMemorySessionRepository();
        AuthenticationSessionRevocationCoordinator revocation = new AuthenticationSessionRevocationCoordinator(
                sessions, AuthenticationTestFakes.clockAt(AuthenticationTestFakes.NOW));
        SoftDeleteUserAccountService service =
                new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, revocation, guard(), PUBLISHER);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.handle(new SoftDeleteUserAccountUseCase.Command(42, "WrongPassword3#")));

        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(0, repository.saveCalls);
        assertEquals(0, sessions.revokeAllCalls);
    }

    @Test
    void closing_the_account_discards_its_pending_email_change_but_a_wrong_password_does_not() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        EmailVerificationTestFakes.Repository pending = new EmailVerificationTestFakes.Repository();
        Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        pending.save(EmailVerification.issue(
                VerificationPurpose.EMAIL_CHANGE,
                "pending-token",
                new UserId(42),
                new Email("new@example.com"),
                fixed,
                Duration.ofHours(24)));
        pending.save(EmailVerification.issue(
                VerificationPurpose.REGISTRATION,
                "registration-token",
                new UserId(42),
                new Email("person@example.com"),
                fixed,
                Duration.ofHours(24)));
        SoftDeleteUserAccountService service =
                new SoftDeleteUserAccountService(repository, hasher, pending, SESSION_REVOCATION, guard(), PUBLISHER);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.handle(new SoftDeleteUserAccountUseCase.Command(42, "WrongPassword3#")));
        assertTrue(pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42).isPresent());

        assertTrue(service.handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD))
                .changed());
        assertTrue(pending.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 42).isEmpty());
        assertTrue(pending.outstandingFor(VerificationPurpose.REGISTRATION, 42).isPresent());
    }

    @Test
    void close_with_a_password_that_could_never_be_valid_is_a_wrong_password_not_a_malformed_request() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        SoftDeleteUserAccountService service =
                new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), PUBLISHER);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.handle(new SoftDeleteUserAccountUseCase.Command(42, "wrong")));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void change_password_with_a_current_password_that_could_never_be_valid_is_a_wrong_password() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        ChangePasswordService service = new ChangePasswordService(repository, hasher, SESSION_REVOCATION, guard());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.handle(new ChangePasswordUseCase.Command(42, "weak", "NewPassword2@")));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void change_password_on_a_locked_account_is_refused_for_its_status_whatever_current_password_is_sent() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount locked = activeAccount(hasher);
        locked.lock();
        repository.add(locked);
        ChangePasswordService service = new ChangePasswordService(repository, hasher, SESSION_REVOCATION, guard());

        for (String presented : new String[] {CURRENT_PASSWORD, "WrongPassword3#", "weak"}) {
            assertThrows(
                    InvalidAccountStateTransitionException.class,
                    () -> service.handle(new ChangePasswordUseCase.Command(42, presented, "NewPassword2@")));
        }
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void closing_an_already_closed_account_is_refused_by_the_password_check() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount closed = activeAccount(hasher);
        closed.softDelete();
        repository.add(closed);
        SoftDeleteUserAccountService service =
                new SoftDeleteUserAccountService(repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), PUBLISHER);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD)));
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void session_revocation_failure_propagates_and_reports_no_change() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        AuthenticationTestFakes.InMemorySessionRepository sessions =
                new AuthenticationTestFakes.InMemorySessionRepository();
        sessions.failOnRevokeAll = true;
        SuspendUserAccountService service = new SuspendUserAccountService(
                repository,
                new AuthenticationSessionRevocationCoordinator(
                        sessions, AuthenticationTestFakes.clockAt(AuthenticationTestFakes.NOW)));

        assertThrows(IllegalStateException.class, () -> service.handle(new SuspendUserAccountUseCase.Command(42)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("existingAccountCommandCases")
    void existing_account_commands_load_once_and_save_only_changes(
            String description, AccountStatus status, ExpectedEffect expectedEffect, ExistingAccountCommand command) {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(account(status, hasher));

        if (expectedEffect == ExpectedEffect.REJECTED) {
            assertThrows(InvalidAccountStateTransitionException.class, () -> command.invoke(repository, hasher, 42));
        } else {
            boolean changed = command.invoke(repository, hasher, 42);
            assertEquals(expectedEffect == ExpectedEffect.CHANGED, changed);
        }

        assertEquals(1, repository.findByIdCalls);
        assertEquals(expectedEffect == ExpectedEffect.CHANGED ? 1 : 0, repository.saveCalls);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("existingAccountCommands")
    void existing_account_commands_fail_after_one_lookup_when_the_account_is_missing(
            String description, ExistingAccountCommand command) {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();

        assertThrows(IllegalArgumentException.class, () -> command.invoke(repository, hasher, 404));
        assertEquals(1, repository.findByIdCalls);
        assertEquals(0, repository.saveCalls);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void query_services_return_query_specific_results_for_every_status(AccountStatus status) {
        UserAccountTestFakes.InMemoryQueryRepository repository = new UserAccountTestFakes.InMemoryQueryRepository();
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        FindUserByIdUseCase.Result expectedById = new FindUserByIdUseCase.Result(
                42,
                "person@example.com",
                "person_42",
                LocalDate.of(1990, 1, 1),
                status.name(),
                status.isAccessible(),
                status == AccountStatus.DELETED ? timestamp : null,
                timestamp,
                timestamp);
        FindUserByEmailUseCase.Result expectedByEmail = new FindUserByEmailUseCase.Result(
                42,
                "person@example.com",
                "person_42",
                LocalDate.of(1990, 1, 1),
                status.name(),
                status.isAccessible(),
                status == AccountStatus.DELETED ? timestamp : null,
                timestamp,
                timestamp);
        repository.addById(expectedById);
        repository.addByEmail(expectedByEmail);

        FindUserByIdUseCase.Result byId = new FindUserByIdService(repository)
                .handle(new FindUserByIdUseCase.Query(42))
                .orElseThrow();
        FindUserByEmailUseCase.Result byEmail = new FindUserByEmailService(repository)
                .handle(new FindUserByEmailUseCase.Query("PERSON@example.com"))
                .orElseThrow();

        assertEquals(expectedById, byId);
        assertEquals(expectedByEmail, byEmail);
        assertEquals(1, repository.findByIdCalls);
        assertEquals(1, repository.findByEmailCalls);
    }

    @Test
    void query_services_return_empty_when_no_account_is_found() {
        UserAccountTestFakes.InMemoryQueryRepository repository = new UserAccountTestFakes.InMemoryQueryRepository();

        assertTrue(new FindUserByIdService(repository)
                .handle(new FindUserByIdUseCase.Query(404))
                .isEmpty());
        assertTrue(new FindUserByEmailService(repository)
                .handle(new FindUserByEmailUseCase.Query("missing@example.com"))
                .isEmpty());
        assertEquals(1, repository.findByIdCalls);
        assertEquals(1, repository.findByEmailCalls);
    }

    @Test
    void query_services_reject_invalid_inputs_before_reading() {
        UserAccountTestFakes.InMemoryQueryRepository repository = new UserAccountTestFakes.InMemoryQueryRepository();

        assertThrows(IllegalArgumentException.class, () -> new FindUserByIdService(repository)
                .handle(new FindUserByIdUseCase.Query(0)));
        assertThrows(IllegalArgumentException.class, () -> new FindUserByEmailService(repository)
                .handle(new FindUserByEmailUseCase.Query("invalid-email")));
        assertEquals(0, repository.findByIdCalls);
        assertEquals(0, repository.findByEmailCalls);
    }

    private static Stream<Arguments> existingAccountCommandCases() {
        return Stream.of(
                Arguments.of("lock changes active", AccountStatus.ACTIVE, ExpectedEffect.CHANGED, LOCK),
                Arguments.of("lock is a locked no-op", AccountStatus.LOCKED, ExpectedEffect.NO_OP, LOCK),
                Arguments.of("lock rejects pending", AccountStatus.PENDING_VERIFICATION, ExpectedEffect.REJECTED, LOCK),
                Arguments.of("unlock changes locked", AccountStatus.LOCKED, ExpectedEffect.CHANGED, UNLOCK),
                Arguments.of("unlock is an active no-op", AccountStatus.ACTIVE, ExpectedEffect.NO_OP, UNLOCK),
                Arguments.of("unlock rejects suspended", AccountStatus.SUSPENDED, ExpectedEffect.REJECTED, UNLOCK),
                Arguments.of("suspend changes active", AccountStatus.ACTIVE, ExpectedEffect.CHANGED, SUSPEND),
                Arguments.of("suspend is a suspended no-op", AccountStatus.SUSPENDED, ExpectedEffect.NO_OP, SUSPEND),
                Arguments.of("suspend rejects locked", AccountStatus.LOCKED, ExpectedEffect.REJECTED, SUSPEND),
                Arguments.of("reinstate changes suspended", AccountStatus.SUSPENDED, ExpectedEffect.CHANGED, REINSTATE),
                Arguments.of("reinstate is an active no-op", AccountStatus.ACTIVE, ExpectedEffect.NO_OP, REINSTATE),
                Arguments.of("reinstate rejects locked", AccountStatus.LOCKED, ExpectedEffect.REJECTED, REINSTATE),
                Arguments.of("soft delete changes active", AccountStatus.ACTIVE, ExpectedEffect.CHANGED, SOFT_DELETE),
                Arguments.of("password changes active", AccountStatus.ACTIVE, ExpectedEffect.CHANGED, CHANGE_PASSWORD),
                Arguments.of("password is an active no-op", AccountStatus.ACTIVE, ExpectedEffect.NO_OP, KEEP_PASSWORD),
                Arguments.of(
                        "password rejects pending",
                        AccountStatus.PENDING_VERIFICATION,
                        ExpectedEffect.REJECTED,
                        CHANGE_PASSWORD),
                Arguments.of("username changes active", AccountStatus.ACTIVE, ExpectedEffect.CHANGED, CHANGE_USERNAME),
                Arguments.of("username is an active no-op", AccountStatus.ACTIVE, ExpectedEffect.NO_OP, KEEP_USERNAME),
                Arguments.of("username rejects locked", AccountStatus.LOCKED, ExpectedEffect.REJECTED, CHANGE_USERNAME),
                Arguments.of(
                        "username rejects suspended",
                        AccountStatus.SUSPENDED,
                        ExpectedEffect.REJECTED,
                        CHANGE_USERNAME));
    }

    private static Stream<Arguments> existingAccountCommands() {
        return Stream.of(
                Arguments.of("lock missing account", LOCK),
                Arguments.of("unlock missing account", UNLOCK),
                Arguments.of("suspend missing account", SUSPEND),
                Arguments.of("reinstate missing account", REINSTATE),
                Arguments.of("soft delete missing account", SOFT_DELETE),
                Arguments.of("change password missing account", CHANGE_PASSWORD),
                Arguments.of("change username missing account", CHANGE_USERNAME));
    }

    private UserAccount pendingAccount(UserAccountTestFakes.DeterministicPasswordHasher hasher) {
        return UserAccount.register(
                new UserId(42),
                new Email("person@example.com"),
                new Username("person_42"),
                new Birthday(LocalDate.of(1990, 1, 1)),
                hasher.hash(new PlainPassword(CURRENT_PASSWORD)));
    }

    private UserAccount activeAccount(UserAccountTestFakes.DeterministicPasswordHasher hasher) {
        UserAccount account = pendingAccount(hasher);
        account.activateAfterVerification();
        return account;
    }

    private void assertRegistrationClassification(AccountStatus status, RegisterUserUseCase.Result result) {
        if (status == AccountStatus.PENDING_VERIFICATION) {
            assertInstanceOf(RegisterUserUseCase.VerificationPending.class, result);
        } else {
            assertInstanceOf(RegisterUserUseCase.AccountAlreadyExists.class, result);
        }
    }

    private UserAccount account(AccountStatus status, UserAccountTestFakes.DeterministicPasswordHasher hasher) {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        return UserAccount.reconstitute(
                new UserId(42),
                new Email("person@example.com"),
                new Username("person_42"),
                new Birthday(LocalDate.of(1990, 1, 1)),
                status,
                status == AccountStatus.DELETED ? timestamp : null,
                hasher.hash(new PlainPassword(CURRENT_PASSWORD)),
                timestamp,
                timestamp,
                0L);
    }

    private RegisterUserUseCase.Command registrationCommand() {
        return new RegisterUserUseCase.Command(
                " PERSON@Example.com ", "person_42", LocalDate.of(1990, 1, 1), CURRENT_PASSWORD);
    }

    private EmailVerificationIssuanceCoordinator issuance() {
        return new EmailVerificationIssuanceCoordinator(
                new EmailVerificationIssuer(
                        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC),
                        () -> "test-token",
                        new EmptyVerificationRepository()),
                (email, token, expiry) -> {});
    }

    private static final class EmptyVerificationRepository implements EmailVerificationRepository {
        @Override
        public void save(EmailVerification verification) {}

        @Override
        public java.util.Optional<EmailVerification> findByPresentedToken(VerificationPurpose purpose, String token) {
            return java.util.Optional.empty();
        }

        @Override
        public void invalidateForUser(VerificationPurpose purpose, UserId userId) {}
    }

    @Test
    void soft_delete_announces_the_deletion_once_with_the_accounts_own_deletion_time() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount account = activeAccount(hasher);
        repository.add(account);
        RecordingPublisher publisher = new RecordingPublisher();
        SoftDeleteUserAccountService service = new SoftDeleteUserAccountService(
                repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), publisher);

        assertTrue(service.handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD))
                .changed());

        assertEquals(List.of(new Published(new UserId(42), account.deletedAt())), publisher.published);
    }

    @Test
    void soft_delete_with_a_wrong_password_announces_nothing() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        RecordingPublisher publisher = new RecordingPublisher();
        SoftDeleteUserAccountService service = new SoftDeleteUserAccountService(
                repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), publisher);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.handle(new SoftDeleteUserAccountUseCase.Command(42, "WrongPassword3#")));

        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void repeated_soft_deletes_of_one_account_announce_exactly_once() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        RecordingPublisher publisher = new RecordingPublisher();
        // one guard per attempt: this test is about announcements, not about the throttle
        for (int attempt = 0; attempt < 100; attempt++) {
            SoftDeleteUserAccountService service = new SoftDeleteUserAccountService(
                    repository, hasher, NO_PENDING_CHANGES, SESSION_REVOCATION, guard(), publisher);
            try {
                service.handle(new SoftDeleteUserAccountUseCase.Command(42, CURRENT_PASSWORD));
            } catch (InvalidCredentialsException expectedOnceTheAccountIsClosed) {
                // the closed account no longer has a usable password, so later attempts are refused
            }
        }

        assertEquals(1, publisher.published.size());
    }

    private record Published(UserId userId, Instant deletedAt) {}

    private static final class RecordingPublisher implements UserAccountEventPublisher {
        private final List<Published> published = new ArrayList<>();

        @Override
        public void publishDeleted(UserId userId, Instant deletedAt) {
            published.add(new Published(userId, deletedAt));
        }
    }

    private enum ExpectedEffect {
        CHANGED,
        NO_OP,
        REJECTED
    }

    @FunctionalInterface
    private interface ExistingAccountCommand {
        boolean invoke(
                UserAccountTestFakes.InMemoryRepository repository,
                UserAccountTestFakes.DeterministicPasswordHasher hasher,
                long userId);
    }
}
