package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestEmailVerificationService implements RequestEmailVerificationUseCase {
    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final EmailVerificationIssuanceCoordinator issuanceCoordinator;

    @Override
    @Transactional
    public Accepted handle(Command command) {
        Email email = new Email(command.email());
        userRepository
                .findByEmail(email)
                .filter(account -> account.status() == AccountStatus.PENDING_VERIFICATION)
                .ifPresent(account -> issuanceCoordinator.issue(account.id(), account.email()));
        return new Accepted();
    }
}
