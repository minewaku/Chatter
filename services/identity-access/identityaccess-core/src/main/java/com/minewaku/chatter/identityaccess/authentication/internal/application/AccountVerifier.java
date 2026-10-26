package com.minewaku.chatter.identityaccess.authentication.internal.application;

import com.minewaku.chatter.identityaccess.user.api.exception.UserAccountNotFoundException;
import com.minewaku.chatter.identityaccess.user.internal.application.ReauthenticationGuard;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

/**
 * Confirms an ACTIVE account and its current password for session-management commands. The rule itself (status, then
 * password) belongs to {@link UserAccount#confirmCredentials}; this class only finds the account and wraps the check in
 * the person's password-confirmation limit, so these commands cannot be used to guess the password either.
 *
 * <p>Each failure is reported as what it is: {@link UserAccountNotFoundException} for a missing account, the account's
 * own exceptions for a blocked account or a wrong password.
 */
@RequiredArgsConstructor
public final class AccountVerifier {

    private final UserAccountRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final ReauthenticationGuard guard;

    public UserAccount verify(long userId, String password) {
        return guard.confirm(userId, () -> {
            UserAccount account =
                    userRepository.findById(new UserId(userId)).orElseThrow(UserAccountNotFoundException::new);
            account.confirmCredentials(password, passwordHasher);
            return account;
        });
    }
}
