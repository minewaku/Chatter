package com.minewaku.chatter.identityaccess.domain.sharedkernel.service;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

public interface PasswordHasher {
    HashedPassword hash(Password rawPassword);
    boolean matchesRawAndHashedPassword(Password password, HashedPassword hashedPassword);
}
