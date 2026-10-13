package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.command.VerifyEmailUseCase;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyEmailService implements VerifyEmailUseCase {
    @NonNull private final EmailVerificationRepository verificationRepository;

    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final Clock clock;

    @Override
    @Transactional
    public Result handle(Command command) {
        if (command == null || command.token() == null || command.token().isBlank()) return new InvalidOrExpired();
        EmailVerification verification =
                verificationRepository.findByPresentedToken(command.token()).orElse(null);
        if (verification == null) return new InvalidOrExpired();
        var now = clock.instant();
        if (verification.isExpired(now)) return new InvalidOrExpired();
        if (verification.isVerified()) return new AlreadyVerified();
        UserAccount account = userRepository.findById(verification.userId()).orElse(null);
        if (account == null
                || account.status() != AccountStatus.PENDING_VERIFICATION
                || !account.email().equals(verification.email())) return new InvalidOrExpired();
        if (!verification.verify(now)) return new InvalidOrExpired();
        account.activateAfterVerification();
        verificationRepository.save(verification);
        userRepository.save(account);
        return new Verified(account.id().value());
    }
}
