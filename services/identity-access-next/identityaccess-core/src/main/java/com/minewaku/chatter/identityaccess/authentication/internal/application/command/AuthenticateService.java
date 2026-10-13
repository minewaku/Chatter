package com.minewaku.chatter.identityaccess.authentication.internal.application.command;

import com.minewaku.chatter.identityaccess.authentication.api.command.AuthenticateUseCase;
import com.minewaku.chatter.identityaccess.authentication.internal.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.authentication.internal.port.AccessTokenIssuer;
import com.minewaku.chatter.identityaccess.authentication.internal.port.RefreshTokenGenerator;
import com.minewaku.chatter.identityaccess.authentication.internal.port.repository.AuthenticationSessionRepository;
import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateService implements AuthenticateUseCase {

    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final PasswordHasher passwordHasher;

    @NonNull private final UniqueStringIdGenerator sessionIds;

    @NonNull private final RefreshTokenGenerator refreshTokens;

    @NonNull private final AccessTokenIssuer accessTokens;

    @NonNull private final AuthenticationSessionRepository sessions;

    @NonNull private final Clock clock;

    @NonNull private final Duration sessionLifetime;

    @Override
    @Transactional
    public Authenticated handle(Command command) {
        AuthenticateUseCase.DeviceInfo device = Objects.requireNonNull(command.deviceInfo(), "device info is required");
        UserAccount account = accessibleAccountMatching(command.email(), command.password());

        String refreshToken = refreshTokens.generate();
        AuthenticationSession session = AuthenticationSession.issue(
                sessionIds.nextId(), account.id(), device.label(), clock.instant(), sessionLifetime);
        String accessToken = accessTokens.issue(account.id(), account.email());

        sessions.create(session, refreshToken);
        return new Authenticated(accessToken, refreshToken);
    }

    private UserAccount accessibleAccountMatching(String email, String password) {
        Email normalizedEmail;
        PlainPassword plainPassword;
        try {
            normalizedEmail = new Email(email);
            plainPassword = new PlainPassword(password);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException();
        }
        UserAccount account =
                userRepository.findByEmail(normalizedEmail).orElseThrow(AuthenticationFailedException::new);
        if (!account.isAccessible() || !passwordHasher.matches(plainPassword, account.passwordHash())) {
            throw new AuthenticationFailedException();
        }
        return account;
    }
}
