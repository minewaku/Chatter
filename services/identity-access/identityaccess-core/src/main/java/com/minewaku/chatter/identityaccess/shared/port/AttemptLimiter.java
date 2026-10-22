package com.minewaku.chatter.identityaccess.shared.port;

import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptOutcome;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;

/**
 * Counts attempts per subject (a normalized email, a user id) within a policy window. The subject is a plain string
 * and is never logged by an implementation.
 *
 * <p>Both operations throw {@link DependencyUnavailableException} when the count cannot be read or written. An
 * implementation never answers "allowed" because it could not check.
 */
public interface AttemptLimiter {

    /** Counts this attempt and says whether it is within the policy. Atomic: concurrent calls cannot both see room. */
    AttemptOutcome hit(AttemptPolicy policy, String subject);

    /** Forgets the subject's count for this policy, after a success. */
    void clear(AttemptPolicy policy, String subject);
}
