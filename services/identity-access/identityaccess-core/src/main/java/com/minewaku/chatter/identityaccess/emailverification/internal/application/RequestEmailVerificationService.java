package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.port.AttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptOutcome;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import com.minewaku.chatter.identityaccess.shared.throttle.ThrottlePolicies;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
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

    @NonNull private final AttemptLimiter attempts;

    @NonNull private final ThrottlePolicies policies;

    @Override
    @Transactional
    public Accepted handle(Command command) {
        Email email = new Email(command.email());
        // Every well-formed address is counted whether or not it has an account, so a refusal reveals nothing. The
        // gap is checked first: a request it refuses never uses up the hourly allowance.
        refuseIfOver(policies.resendGap(), email);
        refuseIfOver(policies.resendHourly(), email);
        userRepository
                .findByEmail(email)
                .filter(account -> account.status() == AccountStatus.PENDING_VERIFICATION)
                .ifPresent(account -> issue(account.id(), account.email()));
        return new Accepted();
    }

    /**
     * A resend is always accepted for a well-formed address. Answering differently when the mail cannot be sent would
     * only happen for accounts that are waiting for verification, which would tell a caller which addresses those
     * are, so a delivery failure is hidden here and the person simply asks again.
     */
    private void issue(UserId userId, Email email) {
        try {
            issuanceCoordinator.issue(userId, email);
        } catch (MailDeliveryException e) {
            // accepted all the same
        }
    }

    private void refuseIfOver(AttemptPolicy policy, Email email) {
        AttemptOutcome outcome = attempts.hit(policy, email.value());
        if (!outcome.allowed()) {
            throw new TooManyAttemptsException(outcome.retryAfter());
        }
    }
}
