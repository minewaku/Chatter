package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.user.api.command.ConfirmEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.PendingEmailChangeRepository;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completes an email change from the token mailed to the new address. The token is the only proof needed, because
 * the link may be opened on another device. Every refusal that could reveal account state is the generic one.
 */
@Service
@RequiredArgsConstructor
public class ConfirmEmailChangeService implements ConfirmEmailChangeUseCase {

    @NonNull private final PendingEmailChangeRepository pendingChanges;

    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final Clock clock;

    @Override
    @Transactional
    public Result handle(Command command) {
        if (command == null || command.token() == null || command.token().isBlank()) {
            return new InvalidOrExpired();
        }
        PendingEmailChange change =
                pendingChanges.findByPresentedToken(command.token()).orElse(null);
        Instant now = clock.instant();
        if (change == null || change.isExpired(now)) {
            return new InvalidOrExpired();
        }
        if (change.isConfirmed()) {
            return new AlreadyConfirmed();
        }
        UserAccount account = userRepository.findById(change.userId()).orElse(null);
        if (account == null || !account.isAccessible()) {
            return new InvalidOrExpired();
        }
        if (account.email().equals(change.newEmail())) {
            // an earlier attempt applied the change but did not get to record it
            markConfirmed(change, now);
            return new AlreadyConfirmed();
        }
        if (userRepository.findByEmail(change.newEmail()).isPresent()) {
            pendingChanges.discardForUser(account.id());
            throw new DuplicateUserAccountException("User account already exists");
        }
        account.applyVerifiedEmail(change.newEmail());
        try {
            userRepository.save(account);
        } catch (DuplicateUserAccountException e) {
            pendingChanges.discardForUser(account.id());
            throw e;
        }
        markConfirmed(change, now);
        return new Confirmed(account.id().value());
    }

    private void markConfirmed(PendingEmailChange change, Instant now) {
        if (change.confirm(now)) {
            pendingChanges.save(change);
        }
    }
}
