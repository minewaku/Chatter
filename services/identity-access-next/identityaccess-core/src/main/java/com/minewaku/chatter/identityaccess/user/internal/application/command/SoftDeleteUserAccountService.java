package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationSessionRevocationCoordinator;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteUserAccountService extends UserAccountCommandService implements SoftDeleteUserAccountUseCase {

    private final PasswordHasher passwordHasher;
    private final EmailVerificationRepository verifications;
    private final AuthenticationSessionRevocationCoordinator sessionRevocation;

    public SoftDeleteUserAccountService(
            UserAccountRepository repository,
            PasswordHasher passwordHasher,
            EmailVerificationRepository verifications,
            AuthenticationSessionRevocationCoordinator sessionRevocation) {
        super(repository);
        this.passwordHasher = passwordHasher;
        this.verifications = verifications;
        this.sessionRevocation = sessionRevocation;
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        boolean changed =
                saveIfChanged(account, account.close(presentedPassword(command.currentPassword()), passwordHasher));
        if (changed) {
            verifications.invalidateForUser(VerificationPurpose.EMAIL_CHANGE, account.id());
            sessionRevocation.revokeAllFor(account.id());
        }
        return new Result(changed);
    }
}
