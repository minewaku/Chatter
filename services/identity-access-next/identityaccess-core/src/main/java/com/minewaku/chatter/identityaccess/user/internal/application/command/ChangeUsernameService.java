package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.command.ChangeUsernameUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A username is a display name, so no password is needed. Uniqueness is enforced by the store on save. */
@Service
public class ChangeUsernameService extends UserAccountCommandService implements ChangeUsernameUseCase {

    public ChangeUsernameService(UserAccountRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        boolean changed = account.changeUsername(new Username(command.newUsername()));
        return new Result(saveIfChanged(account, changed));
    }
}
