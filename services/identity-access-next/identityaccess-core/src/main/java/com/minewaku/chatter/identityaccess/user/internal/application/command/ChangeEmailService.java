package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.command.ChangeEmailUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeEmailService extends UserAccountCommandService implements ChangeEmailUseCase {

    private final PasswordHasher passwordHasher;

    public ChangeEmailService(UserAccountRepository repository, PasswordHasher passwordHasher) {
        super(repository);
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        boolean changed = account.changeEmail(
                new PlainPassword(command.currentPassword()), new Email(command.newEmail()), passwordHasher);
        saveIfChanged(account, changed);
        return new Result(changed, changed);
    }
}
