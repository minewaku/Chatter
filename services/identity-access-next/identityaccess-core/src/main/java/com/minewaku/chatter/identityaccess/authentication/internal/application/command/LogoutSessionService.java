package com.minewaku.chatter.identityaccess.authentication.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.application.AccountVerifier;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutSessionService implements LogoutSessionUseCase {

    private final AccountVerifier accountVerifier;
    private final AuthenticationSessionRepository sessions;
    private final Clock clock;

    public LogoutSessionService(
            UserAccountRepository userRepository,
            PasswordHasher passwordHasher,
            AuthenticationSessionRepository sessions,
            Clock clock) {
        this.accountVerifier = new AccountVerifier(userRepository, passwordHasher);
        this.sessions = Objects.requireNonNull(sessions);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public Result handle(Command command) {
        UserAccount account = accountVerifier.verify(command.userId(), command.currentPassword());
        return new Result(sessions.revokeBySessionId(account.id(), command.sessionId(), clock.instant()));
    }
}
