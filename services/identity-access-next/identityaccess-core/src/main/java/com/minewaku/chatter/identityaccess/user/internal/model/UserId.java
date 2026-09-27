package com.minewaku.chatter.identityaccess.user.internal.model;

public record UserId(long value) {

    public UserId {
        if (value <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }
}
