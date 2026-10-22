package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.emailverification.internal.application.EmailVerificationIssuer;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.port.AttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptOutcome;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import com.minewaku.chatter.identityaccess.shared.throttle.ThrottlePolicies;
import com.minewaku.chatter.identityaccess.user.api.command.RequestEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.application.ReauthenticationGuard;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.EmailChangeNotifier;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

/**
 * Starts an email change: the account keeps its email, and the new address receives a token. The password is
 * checked first so a caller who cannot prove it learns nothing about which addresses are taken.
 */
@Service
public class RequestEmailChangeService extends UserAccountCommandService implements RequestEmailChangeUseCase {

    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationIssuer issuer;
    private final EmailChangeNotifier notifier;
    private final ReauthenticationGuard guard;
    private final AttemptLimiter attempts;
    private final AttemptPolicy requestPolicy;

    public RequestEmailChangeService(
            UserAccountRepository repository,
            PasswordHasher passwordHasher,
            EmailVerificationIssuer issuer,
            EmailChangeNotifier notifier,
            ReauthenticationGuard guard,
            AttemptLimiter attempts,
            ThrottlePolicies policies) {
        super(repository);
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.issuer = issuer;
        this.notifier = notifier;
        this.guard = guard;
        this.attempts = attempts;
        this.requestPolicy = policies.emailChangeRequest();
    }

    @Override
    public Result handle(Command command) {
        return guard.confirm(command.userId(), () -> request(command));
    }

    private Result request(Command command) {
        UserAccount account = getAccount(command.userId());
        Email newEmail = new Email(command.newEmail());
        if (!account.authorizeEmailChange(presentedPassword(command.currentPassword()), newEmail, passwordHasher)) {
            return new Result(false);
        }
        if (repository.findByEmail(newEmail).isPresent()) {
            throw new DuplicateUserAccountException("User account already exists");
        }
        // Counted only now, so a wrong password, an unchanged address, or a taken address never uses up the allowance.
        AttemptOutcome outcome =
                attempts.hit(requestPolicy, Long.toString(account.id().value()));
        if (!outcome.allowed()) {
            throw new TooManyAttemptsException(outcome.retryAfter());
        }
        EmailVerification change = issuer.issue(VerificationPurpose.EMAIL_CHANGE, account.id(), newEmail);
        notifier.sendEmailChange(newEmail, change.rawToken(), change.expiresAt());
        return new Result(true);
    }
}
