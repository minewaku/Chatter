package com.minewaku.chatter.identityaccess.authentication.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.api.command.RefreshAccessTokenUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.AccessTokenIssuer;
import com.minewaku.chatter.identityaccess.authentication.internal.port.RefreshTokenGenerator;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renews access: issues a new access credential and rotates the refresh credential, giving the session a new full
 * lifetime. The access credential is issued before the rotation so that a failing issuer leaves the session and the
 * presented credential untouched; an unsuccessful rotation discards the issued access credential unseen.
 */
@Service
@RequiredArgsConstructor
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    @NonNull private final AuthenticationSessionRepository sessions;

    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final AccessTokenIssuer accessTokens;

    @NonNull private final RefreshTokenGenerator refreshTokens;

    @NonNull private final Clock clock;

    @NonNull private final Duration sessionLifetime;

    @Override
    @Transactional
    public Refreshed handle(Command command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new AuthenticationFailedException();
        }
        Instant now = clock.instant();
        AuthenticationSession session = sessions.findByPresentedToken(command.refreshToken())
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(AuthenticationFailedException::new);
        UserAccount owner = userRepository
                .findById(session.userId())
                .filter(UserAccount::isAccessible)
                .orElseThrow(AuthenticationFailedException::new);

        String accessToken = accessTokens.issue(owner.id(), owner.email(), session.sessionId());
        String newRefreshToken = refreshTokens.generate();
        boolean rotated = sessions.rotate(
                session.sessionId(), command.refreshToken(), newRefreshToken, now, now.plus(sessionLifetime));
        if (!rotated) {
            throw new AuthenticationFailedException();
        }
        return new Refreshed(accessToken, newRefreshToken);
    }
}
