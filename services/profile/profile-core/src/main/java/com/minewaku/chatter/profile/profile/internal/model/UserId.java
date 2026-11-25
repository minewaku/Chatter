package com.minewaku.chatter.profile.profile.internal.model;

/** The account id carried by the access token or by an account-deleted notice. Always positive. */
public record UserId(long value) {

    public UserId {
        if (value <= 0) {
            throw new IllegalArgumentException("user id must be positive");
        }
    }

    public static UserId of(long value) {
        return new UserId(value);
    }
}
