package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.internal.application.AuthenticationSessionRevocationCoordinator;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteUserAccountService extends UserAccountCommandService implements SoftDeleteUserAccountUseCase {

    private final AuthenticationSessionRevocationCoordinator sessionRevocation;

    public SoftDeleteUserAccountService(
            UserAccountRepository repository, AuthenticationSessionRevocationCoordinator sessionRevocation) {
        super(repository);
        this.sessionRevocation = sessionRevocation;
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        boolean changed = saveIfChanged(account, account.softDelete());
        if (changed) {
            sessionRevocation.revokeAllFor(account.id());
        }
        return new Result(changed);
    }
}
