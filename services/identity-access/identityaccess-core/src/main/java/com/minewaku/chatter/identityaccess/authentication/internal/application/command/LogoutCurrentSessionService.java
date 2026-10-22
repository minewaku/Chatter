package com.minewaku.chatter.identityaccess.authentication.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutCurrentSessionService implements LogoutCurrentSessionUseCase {

    @NonNull private final AuthenticationSessionRepository sessions;

    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final Clock clock;

    @Override
    @Transactional
    public Result handle(Command command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            return new Result(false);
        }
        Instant now = clock.instant();
        return sessions.findByPresentedToken(command.refreshToken())
                .filter(session -> session.isUsable(now))
                .filter(session -> ownerIsAccessible(session))
                .map(session -> new Result(sessions.revokeBySessionId(session.userId(), session.sessionId(), now)))
                .orElse(new Result(false));
    }

    private boolean ownerIsAccessible(AuthenticationSession session) {
        return userRepository
                .findById(session.userId())
                .map(UserAccount::isAccessible)
                .orElse(false);
    }
}
