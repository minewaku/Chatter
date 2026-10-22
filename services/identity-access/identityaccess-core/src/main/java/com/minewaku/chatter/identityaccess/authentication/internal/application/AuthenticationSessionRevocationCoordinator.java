package com.minewaku.chatter.identityaccess.authentication.internal.application;

import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Synchronous entry point used by account-change services; propagates repository failures. */
@Service
@RequiredArgsConstructor
public class AuthenticationSessionRevocationCoordinator {

    @NonNull private final AuthenticationSessionRepository sessions;

    @NonNull private final Clock clock;

    public int revokeAllFor(UserId userId) {
        return sessions.revokeAllByUserId(userId, clock.instant());
    }
}
