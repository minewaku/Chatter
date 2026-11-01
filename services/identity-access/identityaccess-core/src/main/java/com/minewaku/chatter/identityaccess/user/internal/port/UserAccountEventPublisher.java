package com.minewaku.chatter.identityaccess.user.internal.port;

import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Instant;

public interface UserAccountEventPublisher {

    /** Announces that the account was soft-deleted. Must run inside the deletion's transaction. */
    void publishDeleted(UserId userId, Instant deletedAt);
}
