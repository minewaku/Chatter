package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationIssuanceCoordinator;
import com.minewaku.chatter.identityaccess.shared.port.TimeBasedIdGenerator;
import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final UserAccountRepository repository;
    private final TimeBasedIdGenerator userIdGenerator;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationIssuanceCoordinator verificationIssuance;

    @Override
    @Transactional
    public Result handle(Command command) {
        Email email = new Email(command.email());
        Username username = new Username(command.username());
        Birthday birthday = new Birthday(command.birthday());
        PlainPassword password = new PlainPassword(command.password());
        Result existingResult =
                repository.findByEmail(email).map(this::registrationResult).orElse(null);
        if (existingResult != null) {
            return existingResult;
        }
        UserAccount account = UserAccount.register(
                new UserId(userIdGenerator.nextId()), email, username, birthday, passwordHasher.hash(password));
        repository.save(account);
        verificationIssuance.issue(account.id(), account.email());
        return new Registered(account.id().value());
    }

    private Result registrationResult(UserAccount account) {
        return account.status() == AccountStatus.PENDING_VERIFICATION
                ? new VerificationPending()
                : new AccountAlreadyExists();
    }
}
