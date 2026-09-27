package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService extends UserAccountCommandService implements ChangePasswordUseCase {

    private final PasswordHasher passwordHasher;

    public ChangePasswordService(UserAccountRepository repository, PasswordHasher passwordHasher) {
        super(repository);
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        boolean changed = account.changePassword(
                new PlainPassword(command.currentPassword()), new PlainPassword(command.newPassword()), passwordHasher);
        return new Result(saveIfChanged(account, changed));
    }
}
