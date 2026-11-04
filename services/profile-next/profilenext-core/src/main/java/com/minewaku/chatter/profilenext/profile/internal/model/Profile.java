package com.minewaku.chatter.profilenext.profile.internal.model;

import java.time.Instant;

/**
 * What the service knows about one user. {@code createdAt} and {@code updatedAt} are null while no row exists, which is
 * how a profile that was never saved is told apart from a saved one.
 */
public record Profile(
        UserId userId,
        String displayName,
        String bio,
        StoredImage avatar,
        StoredImage banner,
        Instant createdAt,
        Instant updatedAt) {

    /** The profile of a user who never saved anything: empty text, no images, no timestamps. */
    public static Profile defaultsFor(UserId userId) {
        return new Profile(userId, "", "", null, null, null, null);
    }
}
