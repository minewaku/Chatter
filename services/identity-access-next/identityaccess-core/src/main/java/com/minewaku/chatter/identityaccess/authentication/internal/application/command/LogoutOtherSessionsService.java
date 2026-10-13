package com.minewaku.chatter.identityaccess.authentication.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutOtherSessionsUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.application.AccountVerifier;
import com.minewaku.chatter.identityaccess.authentication.internal.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutOtherSessionsService implements LogoutOtherSessionsUseCase {

    private final AccountVerifier accountVerifier;
    private final AuthenticationSessionRepository sessions;
    private final Clock clock;

    public LogoutOtherSessionsService(
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
        Instant now = clock.instant();
        AuthenticationSession current = sessions.findBySessionId(command.currentSessionId())
                .filter(session -> session.userId().equals(account.id()) && session.isUsable(now))
                .orElseThrow(AuthenticationFailedException::new);
        return new Result(sessions.revokeOtherSessions(account.id(), current.sessionId(), now));
    }
}
