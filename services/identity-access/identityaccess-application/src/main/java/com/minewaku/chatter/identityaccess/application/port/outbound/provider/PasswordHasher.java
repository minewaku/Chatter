package com.minewaku.chatter.identityaccess.application.port.outbound.provider;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

public interface PasswordHasher {
    HashedPassword hash(Password rawPassword);
    boolean matchesRawAndHashedPassword(Password password, HashedPassword hashedPassword);
}
