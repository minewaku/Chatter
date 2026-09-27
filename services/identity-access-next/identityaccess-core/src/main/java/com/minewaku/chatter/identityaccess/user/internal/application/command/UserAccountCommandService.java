package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;

abstract class UserAccountCommandService {

    private final UserAccountRepository repository;

    UserAccountCommandService(UserAccountRepository repository) {
        this.repository = repository;
    }

    protected UserAccount getAccount(long userId) {
        return repository
                .findById(new UserId(userId))
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
    }

    protected boolean saveIfChanged(UserAccount account, boolean changed) {
        if (changed) {
            repository.save(account);
        }
        return changed;
    }
}
