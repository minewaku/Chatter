package com.minewaku.chatter.identityaccess.authentication.internal.port;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;

public interface AccessTokenIssuer {

    /** Returns an access credential for the account; signing and claims are adapter details. */
    String issue(UserId userId, Email email);
}
