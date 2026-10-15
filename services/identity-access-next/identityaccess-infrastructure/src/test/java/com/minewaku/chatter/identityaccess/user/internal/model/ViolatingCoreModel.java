package com.minewaku.chatter.identityaccess.user.internal.model;

import com.minewaku.chatter.identityaccess.infrastructure.security.TokenDigest;

/**
 * Deliberately wrong: a core class that imports an infrastructure type. Used only by
 * {@code ArchitectureRuleNegativeTest} to prove the structure rules fail on a violation (SC-006).
 */
public class ViolatingCoreModel {

    public String digest() {
        return TokenDigest.sha256Hex("violation");
    }
}
