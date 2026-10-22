package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The one place that issues a proof of mailbox ownership: the earlier proof of the same purpose is invalidated, a token
 * is generated, and the new proof is stored. Sending the token is left to each flow, because the wording differs.
 */
@Service
public class EmailVerificationIssuer {
    public static final Duration LIFETIME = Duration.ofHours(24);

    private final Clock clock;
    private final Duration lifetime;
    private final UniqueStringIdGenerator tokenGenerator;
    private final EmailVerificationRepository verificationRepository;

    @Autowired
    public EmailVerificationIssuer(
            Clock clock, UniqueStringIdGenerator tokenGenerator, EmailVerificationRepository verificationRepository) {
        this(clock, LIFETIME, tokenGenerator, verificationRepository);
    }

    public EmailVerificationIssuer(
            Clock clock,
            Duration lifetime,
            UniqueStringIdGenerator tokenGenerator,
            EmailVerificationRepository verificationRepository) {
        this.clock = Objects.requireNonNull(clock);
        EmailVerification.requirePositive(lifetime);
        this.lifetime = lifetime;
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator);
        this.verificationRepository = Objects.requireNonNull(verificationRepository);
    }

    public EmailVerification issue(VerificationPurpose purpose, UserId userId, Email email) {
        verificationRepository.invalidateForUser(purpose, userId);
        EmailVerification verification =
                EmailVerification.issue(purpose, tokenGenerator.nextId(), userId, email, clock, lifetime);
        verificationRepository.save(verification);
        return verification;
    }
}
