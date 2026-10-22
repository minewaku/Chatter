package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationSessionRevocationCoordinator;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.internal.application.ReauthenticationGuard;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService extends UserAccountCommandService implements ChangePasswordUseCase {

    private final PasswordHasher passwordHasher;
    private final AuthenticationSessionRevocationCoordinator sessionRevocation;
    private final ReauthenticationGuard guard;

    public ChangePasswordService(
            UserAccountRepository repository,
            PasswordHasher passwordHasher,
            AuthenticationSessionRevocationCoordinator sessionRevocation,
            ReauthenticationGuard guard) {
        super(repository);
        this.passwordHasher = passwordHasher;
        this.sessionRevocation = sessionRevocation;
        this.guard = guard;
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        return guard.confirm(command.userId(), () -> {
            UserAccount account = getAccount(command.userId());
            boolean changed = account.changePassword(
                    new PlainPassword(command.currentPassword()),
                    new PlainPassword(command.newPassword()),
                    passwordHasher);
            boolean saved = saveIfChanged(account, changed);
            if (saved) {
                sessionRevocation.revokeAllFor(account.id());
            }
            return new Result(saved);
        });
    }
}
