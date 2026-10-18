package com.minewaku.chatter.identityaccess.user.internal.application.command;

import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.api.command.RequestEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.EmailChangeNotifier;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.PendingEmailChangeRepository;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;

/**
 * Starts an email change: the account keeps its email, and the new address receives a token. The password is
 * checked first so a caller who cannot prove it learns nothing about which addresses are taken.
 */
@Service
public class RequestEmailChangeService extends UserAccountCommandService implements RequestEmailChangeUseCase {

    /** Same lifetime as registration verification tokens. */
    static final Duration LIFETIME = Duration.ofHours(24);

    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final PendingEmailChangeRepository pendingChanges;
    private final EmailChangeNotifier notifier;
    private final Clock clock;
    private final UniqueStringIdGenerator tokenGenerator;

    public RequestEmailChangeService(
            UserAccountRepository repository,
            PasswordHasher passwordHasher,
            PendingEmailChangeRepository pendingChanges,
            EmailChangeNotifier notifier,
            Clock clock,
            UniqueStringIdGenerator tokenGenerator) {
        super(repository);
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.pendingChanges = pendingChanges;
        this.notifier = notifier;
        this.clock = clock;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Result handle(Command command) {
        UserAccount account = getAccount(command.userId());
        Email newEmail = new Email(command.newEmail());
        if (!account.authorizeEmailChange(presentedPassword(command.currentPassword()), newEmail, passwordHasher)) {
            return new Result(false);
        }
        if (repository.findByEmail(newEmail).isPresent()) {
            throw new DuplicateUserAccountException("User account already exists");
        }
        pendingChanges.discardForUser(account.id());
        PendingEmailChange change =
                PendingEmailChange.issue(tokenGenerator.nextId(), account.id(), newEmail, clock, LIFETIME);
        pendingChanges.save(change);
        notifier.sendEmailChange(newEmail, change.rawToken(), change.expiresAt());
        return new Result(true);
    }
}
