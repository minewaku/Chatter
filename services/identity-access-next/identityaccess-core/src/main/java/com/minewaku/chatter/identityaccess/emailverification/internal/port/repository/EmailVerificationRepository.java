package com.minewaku.chatter.identityaccess.emailverification.internal.port.repository;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.Optional;

public interface EmailVerificationRepository {
    void save(EmailVerification verification);

    Optional<EmailVerification> findByPresentedToken(String token);

    void invalidateForUser(UserId userId);
}
