package com.minewaku.chatter.identityaccess.user.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.user.api.command.ActivateAfterVerificationUseCase;
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
import com.minewaku.chatter.identityaccess.user.internal.application.command.ActivateAfterVerificationService;
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
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccountView;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserAccountApplicationServiceTest {

    private static final String CURRENT_PASSWORD = "Password1!";

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
    void registration_returns_typed_outcomes_without_duplicate_saves() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.SequentialUserIdGenerator ids = new UserAccountTestFakes.SequentialUserIdGenerator(100);
        RegisterUserUseCase.Command command = registrationCommand();

        UserAccountTestFakes.InMemoryRepository newRepository = new UserAccountTestFakes.InMemoryRepository();
        RegisterUserUseCase.Result registered = new RegisterUserService(newRepository, ids, hasher).handle(command);
        assertEquals(new RegisterUserUseCase.Registered(100), registered);
        assertEquals(1, newRepository.saveCalls);

        UserAccountTestFakes.InMemoryRepository pendingRepository = new UserAccountTestFakes.InMemoryRepository();
        pendingRepository.add(pendingAccount(hasher));
        RegisterUserUseCase.Result pending = new RegisterUserService(pendingRepository, ids, hasher).handle(command);
        assertInstanceOf(RegisterUserUseCase.VerificationPending.class, pending);
        assertEquals(0, pendingRepository.saveCalls);

        UserAccountTestFakes.InMemoryRepository activeRepository = new UserAccountTestFakes.InMemoryRepository();
        activeRepository.add(activeAccount(hasher));
        RegisterUserUseCase.Result active = new RegisterUserService(activeRepository, ids, hasher).handle(command);
        assertInstanceOf(RegisterUserUseCase.AccountAlreadyActive.class, active);
        assertEquals(0, activeRepository.saveCalls);
    }

    @Test
    void registration_rereads_email_after_a_uniqueness_race() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.duplicateOnNextSave = true;
        repository.accountToExposeAfterDuplicate = pendingAccount(hasher);

        RegisterUserUseCase.Result result = new RegisterUserService(
                        repository, new UserAccountTestFakes.SequentialUserIdGenerator(100), hasher)
                .handle(registrationCommand());

        assertInstanceOf(RegisterUserUseCase.VerificationPending.class, result);
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void state_command_services_save_only_actual_changes() {
        UserAccountTestFakes.DeterministicPasswordHasher hasher =
                new UserAccountTestFakes.DeterministicPasswordHasher();
        UserAccountTestFakes.InMemoryRepository repository = new UserAccountTestFakes.InMemoryRepository();
        repository.add(pendingAccount(hasher));

        assertTrue(new ActivateAfterVerificationService(repository)
                .handle(new ActivateAfterVerificationUseCase.Command(42))
                .changed());
        assertFalse(new ActivateAfterVerificationService(repository)
                .handle(new ActivateAfterVerificationUseCase.Command(42))
                .changed());
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
        assertEquals(6, repository.saveCalls);
    }

    @Test
    void query_services_read_immutable_projections_only_through_view_repository() {
        UserAccountTestFakes.InMemoryViewRepository repository = new UserAccountTestFakes.InMemoryViewRepository();
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        repository.add(new UserAccountView(
                42,
                "person@example.com",
                "person_42",
                LocalDate.of(1990, 1, 1),
                "ACTIVE",
                true,
                null,
                timestamp,
                timestamp));

        FindUserByIdUseCase.Result byId = new FindUserByIdService(repository)
                .handle(new FindUserByIdUseCase.Query(42))
                .orElseThrow();
        FindUserByEmailUseCase.Result byEmail = new FindUserByEmailService(repository)
                .handle(new FindUserByEmailUseCase.Query("PERSON@example.com"))
                .orElseThrow();

        assertEquals("person_42", byId.username());
        assertEquals("person@example.com", byEmail.email());
        assertEquals(1, repository.findByIdCalls);
        assertEquals(1, repository.findByEmailCalls);
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

    private RegisterUserUseCase.Command registrationCommand() {
        return new RegisterUserUseCase.Command(
                "person@example.com", "person_42", LocalDate.of(1990, 1, 1), CURRENT_PASSWORD);
    }
}
