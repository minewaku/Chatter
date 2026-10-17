package com.minewaku.chatter.identityaccess.authentication.internal.port;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;

public interface AccessTokenIssuer {

    /**
     * Returns an access credential for the account, bound to the session it was issued for so that revoking the
     * session can refuse it; signing and claims are adapter details.
     */
    String issue(UserId userId, Email email, String sessionId);
}
