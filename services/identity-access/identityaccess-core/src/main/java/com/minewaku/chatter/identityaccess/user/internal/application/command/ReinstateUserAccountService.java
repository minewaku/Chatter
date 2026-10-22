package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.command.ReinstateUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReinstateUserAccountService extends UserAccountCommandService implements ReinstateUserAccountUseCase {

    public ReinstateUserAccountService(UserAccountRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        return new Result(saveIfChanged(account, account.reinstate()));
    }
}
