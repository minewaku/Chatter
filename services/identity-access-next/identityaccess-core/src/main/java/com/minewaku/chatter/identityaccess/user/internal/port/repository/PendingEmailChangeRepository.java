package com.minewaku.chatter.identityaccess.user.internal.port.repository;

import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.Optional;

public interface PendingEmailChangeRepository {

    /** Stores the change. An account has at most one outstanding change; callers discard the old one first. */
    void save(PendingEmailChange change);

    Optional<PendingEmailChange> findByPresentedToken(String token);

    /** Drops the account's outstanding change, unless it was already confirmed. */
    void discardForUser(UserId userId);
}
