package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.command.UnlockUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnlockUserAccountService extends UserAccountCommandService implements UnlockUserAccountUseCase {

    public UnlockUserAccountService(UserAccountRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        return new Result(saveIfChanged(account, account.unlock()));
    }
}
