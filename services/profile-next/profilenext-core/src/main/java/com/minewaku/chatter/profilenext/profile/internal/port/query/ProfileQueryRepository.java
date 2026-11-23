package com.minewaku.chatter.profilenext.profile.internal.port.query;

import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProfileQueryRepository {

    Optional<ProfileView> findByUserId(UserId userId);

    /**
     * A profile as a reader sees it: plain values read straight from storage, with no value objects, since nothing is
     * being validated on the way out. A name or bio that was never set is an empty string; an image is non-null when
     * its slot holds one.
     */
    record ProfileView(
            long userId,
            String displayName,
            String bio,
            UUID avatar,
            UUID banner,
            Instant createdAt,
            Instant updatedAt) {

        /** What a user who never saved anything looks like: no name, empty bio, no images, no timestamps. */
        public static ProfileView defaultsFor(long userId) {
            return new ProfileView(userId, "", "", null, null, null, null);
        }
    }
}
