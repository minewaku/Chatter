package com.minewaku.chatter.identityaccess.authentication.internal.port.query;

import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.List;

public interface AuthenticationSessionViewRepository {

    /** Retained sessions of this user only, ordered by issuedAt descending then sessionId ascending. */
    List<FindSessionsQuery.SessionView> findByUserId(UserId userId);
}
