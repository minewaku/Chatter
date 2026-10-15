package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user;

import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;

/**
 * Deliberately wrong: a port implementation outside its documented package. Used only by
 * {@code ArchitectureRuleNegativeTest} to prove the port map check fails on a misplaced adapter (SC-006).
 */
public class MisplacedAdapter implements PasswordHasher {

    @Override
    public PasswordHash hash(PlainPassword password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean matches(PlainPassword password, PasswordHash passwordHash) {
        throw new UnsupportedOperationException();
    }
}
