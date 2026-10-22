package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.VerificationNotifier;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Issues a registration proof and mails it. */
@Service
public class EmailVerificationIssuanceCoordinator {
    private final EmailVerificationIssuer issuer;
    private final VerificationNotifier notifier;

    public EmailVerificationIssuanceCoordinator(EmailVerificationIssuer issuer, VerificationNotifier notifier) {
        this.issuer = Objects.requireNonNull(issuer);
        this.notifier = Objects.requireNonNull(notifier);
    }

    public void issue(UserId userId, Email email) {
        EmailVerification verification = issuer.issue(VerificationPurpose.REGISTRATION, userId, email);
        notifier.sendVerification(email, verification.rawToken(), verification.expiresAt());
    }
}
