package com.minewaku.chatter.identityaccess.authentication.internal.application;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

/** Confirms an ACTIVE account and its current password for session-management commands. */
@RequiredArgsConstructor
public final class AccountVerifier {

    private final UserAccountRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserAccount verify(long userId, String password) {
        UserAccount account;
        PlainPassword plainPassword;
        try {
            account = userRepository.findById(new UserId(userId)).orElseThrow(AuthenticationFailedException::new);
            plainPassword = new PlainPassword(password);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException();
        }
        if (!account.isAccessible() || !passwordHasher.matches(plainPassword, account.passwordHash())) {
            throw new AuthenticationFailedException();
        }
        return account;
    }
}
