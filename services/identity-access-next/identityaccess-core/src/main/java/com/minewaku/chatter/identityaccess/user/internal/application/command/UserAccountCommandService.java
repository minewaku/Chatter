package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
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

    /**
     * The password a caller presents to confirm an action. A real password always satisfies the password policy, so
     * input that does not is simply a wrong password, not a malformed request.
     */
    protected PlainPassword presentedPassword(String raw) {
        try {
            return new PlainPassword(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
    }

    protected boolean saveIfChanged(UserAccount account, boolean changed) {
        if (changed) {
            repository.save(account);
        }
        return changed;
    }
}
