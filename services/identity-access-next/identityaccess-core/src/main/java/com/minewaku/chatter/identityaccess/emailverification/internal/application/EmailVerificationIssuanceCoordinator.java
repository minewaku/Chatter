package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.VerificationNotifier;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationIssuanceCoordinator {
    public static final Duration DEFAULT_LIFETIME = Duration.ofHours(24);

    private final Clock clock;
    private final Duration lifetime;
    private final UniqueStringIdGenerator tokenGenerator;
    private final EmailVerificationRepository verificationRepository;
    private final VerificationNotifier notifier;

    @Autowired
    public EmailVerificationIssuanceCoordinator(
            Clock clock,
            UniqueStringIdGenerator tokenGenerator,
            EmailVerificationRepository verificationRepository,
            VerificationNotifier notifier) {
        this(clock, DEFAULT_LIFETIME, tokenGenerator, verificationRepository, notifier);
    }

    public EmailVerificationIssuanceCoordinator(
            Clock clock,
            Duration lifetime,
            UniqueStringIdGenerator tokenGenerator,
            EmailVerificationRepository verificationRepository,
            VerificationNotifier notifier) {
        this.clock = Objects.requireNonNull(clock);
        EmailVerification.requirePositive(lifetime);
        this.lifetime = lifetime;
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator);
        this.verificationRepository = Objects.requireNonNull(verificationRepository);
        this.notifier = Objects.requireNonNull(notifier);
    }

    public void issue(UserId userId, Email email) {
        verificationRepository.invalidateForUser(userId);
        String token = tokenGenerator.nextId();
        EmailVerification verification = EmailVerification.issue(token, userId, email, clock, lifetime);
        verificationRepository.save(verification);
        notifier.sendVerification(email, token, verification.expiresAt());
    }
}
