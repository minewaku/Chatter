package com.minewaku.chatter.identityaccess.emailverification.internal.port.repository;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.Optional;

public interface EmailVerificationRepository {

    /** Stores the proof under its own purpose, apart from proofs of any other purpose. */
    void save(EmailVerification verification);

    /** Never returns a proof issued for a different purpose. */
    Optional<EmailVerification> findByPresentedToken(VerificationPurpose purpose, String token);

    /** Drops the user's outstanding proof of this purpose, unless it was already verified. Other purposes are untouched. */
    void invalidateForUser(VerificationPurpose purpose, UserId userId);
}
