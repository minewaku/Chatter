package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
abstract class UserAccountCommandService {

    private final UserAccountRepository repository;

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
