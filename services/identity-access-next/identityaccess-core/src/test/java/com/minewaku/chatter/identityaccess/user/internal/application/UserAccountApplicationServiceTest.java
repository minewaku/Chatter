package com.minewaku.chatter.identityaccess.user.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.user.api.command.ChangeEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ChangeUsernameUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.LockUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ReinstateUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SuspendUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.UnlockUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.application.command.ChangeEmailService;
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
import com.minewaku.chatter.identityaccess.user.internal.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class UserAccountApplicationServiceTest {

    private static final String CURRENT_PASSWORD = "Password1!";
    private static final ExistingAccountCommand LOCK =
            (repository, hasher, userId) -> new LockUserAccountService(repository)
                    .handle(new LockUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand UNLOCK =
            (repository, hasher, userId) -> new UnlockUserAccountService(repository)
                    .handle(new UnlockUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand SUSPEND =
            (repository, hasher, userId) -> new SuspendUserAccountService(repository)
                    .handle(new SuspendUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand REINSTATE =
            (repository, hasher, userId) -> new ReinstateUserAccountService(repository)
                    .handle(new ReinstateUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand SOFT_DELETE =
            (repository, hasher, userId) -> new SoftDeleteUserAccountService(repository)
                    .handle(new SoftDeleteUserAccountUseCase.Command(userId))
                    .changed();
    private static final ExistingAccountCommand CHANGE_PASSWORD =
            (repository, hasher, userId) -> new ChangePasswordService(repository, hasher)
                    .handle(new ChangePasswordUseCase.Command(userId, CURRENT_PASSWORD, "NewPassword2@"))
                    .changed();
    private static final ExistingAccountCommand KEEP_PASSWORD =
            (repository, hasher, userId) -> new ChangePasswordService(repository, hasher)
                    .handle(new ChangePasswordUseCase.Command(userId, CURRENT_PASSWORD, CURRENT_PASSWORD))
                    .changed();
    private static final ExistingAccountCommand CHANGE_USERNAME =
            (repository, hasher, userId) -> new ChangeUsernameService(repository, hasher)
                    .handle(new ChangeUsernameUseCase.Command(userId, CURRENT_PASSWORD, "new_person"))
                    .changed();
    private static final ExistingAccountCommand KEEP_USERNAME =
            (repository, hasher, userId) -> new ChangeUsernameService(repository, hasher)
                    .handle(new ChangeUsernameUseCase.Command(userId, CURRENT_PASSWORD, "person_42"))
                    .changed();
    private static final ExistingAccountCommand CHANGE_EMAIL =
            (repository, hasher, userId) -> new ChangeEmailService(repository, hasher)
                    .handle(new ChangeEmailUseCase.Command(userId, CURRENT_PASSWORD, "new@example.com"))
                    .changed();
    private static final ExistingAccountCommand KEEP_EMAIL =
            (repository, hasher, userId) -> new ChangeEmailService(repository, hasher)
                    .handle(new ChangeEmailUseCase.Command(userId, CURRENT_PASSWORD, " PERSON@example.com "))
                    .changed();

    @Test
    void change_password_hashes_and_saves_once_only_when_value_changes() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        UserAccount account = activeAccount(hasher);
        repository.add(account);
        ChangePasswordService service = new ChangePasswordService(repository, hasher);

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
    void change_username_and_email_save_only_for_new_values() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(activeAccount(hasher));
        ChangeUsernameService usernameService = new ChangeUsernameService(repository, hasher);

        assertTrue(usernameService
                .handle(new ChangeUsernameUseCase.Command(42, CURRENT_PASSWORD, "new_person"))
                .changed());
        assertFalse(usernameService
                .handle(new ChangeUsernameUseCase.Command(42, CURRENT_PASSWORD, "new_person"))
                .changed());
        assertEquals(1, repository.saveCalls);

        UserAccountTestFakes.InMemoryRepository emailRepository = new UserAccountTestFakes.InMemoryRepository();
        emailRepository.add(activeAccount(hasher));
        ChangeEmailService emailService = new ChangeEmailService(emailRepository, hasher);
        ChangeEmailUseCase.Result unchanged =
                emailService.handle(new ChangeEmailUseCase.Command(42, CURRENT_PASSWORD, " PERSON@example.com "));
        ChangeEmailUseCase.Result changed =
                emailService.handle(new ChangeEmailUseCase.Command(42, CURRENT_PASSWORD, "New@Example.com"));

        assertFalse(unchanged.changed());
        assertFalse(unchanged.verificationRequired());
        assertTrue(changed.changed());
        assertTrue(changed.verificationRequired());
        assertEquals(1, emailRepository.saveCalls);
    }

    @Test
    void registration_creates_and_saves_one_pending_account_when_email_is_available() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();

        RegisterUserUseCase.Result registered =
                new RegisterUserService(repository, ids, hasher).handle(registrationCommand());

        assertEquals(new RegisterUserUseCase.Registered(100), registered);
        assertEquals(1, repository.findByEmailCalls);
        assertEquals(1, ids.calls);
        assertEquals(1, repository.saveCalls);
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
                new RegisterUserService(repository, ids, hasher).handle(registrationCommand());

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
        RegisterUserService service = new RegisterUserService(repository, ids, hasher);

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
        assertTrue(new SuspendUserAccountService(repository)
                .handle(new SuspendUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new SuspendUserAccountService(repository)
                .handle(new SuspendUserAccountUseCase.Command(42))
                .changed());
        assertTrue(new ReinstateUserAccountService(repository)
                .handle(new ReinstateUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new ReinstateUserAccountService(repository)
                .handle(new ReinstateUserAccountUseCase.Command(42))
                .changed());
        assertTrue(new SoftDeleteUserAccountService(repository)
                .handle(new SoftDeleteUserAccountUseCase.Command(42))
                .changed());
        assertFalse(new SoftDeleteUserAccountService(repository)
                .handle(new SoftDeleteUserAccountUseCase.Command(42))
                .changed());
        assertEquals(5, repository.saveCalls);
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
                Arguments.of(
                        "soft delete is a deleted no-op", AccountStatus.DELETED, ExpectedEffect.NO_OP, SOFT_DELETE),
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
                Arguments.of("email changes active", AccountStatus.ACTIVE, ExpectedEffect.CHANGED, CHANGE_EMAIL),
                Arguments.of("email is an active no-op", AccountStatus.ACTIVE, ExpectedEffect.NO_OP, KEEP_EMAIL),
                Arguments.of(
                        "email rejects suspended", AccountStatus.SUSPENDED, ExpectedEffect.REJECTED, CHANGE_EMAIL));
    }

    private static Stream<Arguments> existingAccountCommands() {
        return Stream.of(
                Arguments.of("lock missing account", LOCK),
                Arguments.of("unlock missing account", UNLOCK),
                Arguments.of("suspend missing account", SUSPEND),
                Arguments.of("reinstate missing account", REINSTATE),
                Arguments.of("soft delete missing account", SOFT_DELETE),
                Arguments.of("change password missing account", CHANGE_PASSWORD),
                Arguments.of("change username missing account", CHANGE_USERNAME),
                Arguments.of("change email missing account", CHANGE_EMAIL));
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
