package com.minewaku.chatter.identityaccess.user.internal.port;

import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;

public interface PasswordHasher {

    PasswordHash hash(PlainPassword password);

    boolean matches(PlainPassword password, PasswordHash passwordHash);
}
