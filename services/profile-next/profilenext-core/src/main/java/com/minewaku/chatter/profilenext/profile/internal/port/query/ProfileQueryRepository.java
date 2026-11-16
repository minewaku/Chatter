package com.minewaku.chatter.profilenext.profile.internal.port.query;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Optional;

public interface ProfileQueryRepository {

    Optional<ProfileView> findByUserId(UserId userId);

    /** A profile as a reader sees it. An image is present (non-null) when its slot holds one. */
    record ProfileView(
            UserId userId,
            DisplayName displayName,
            Bio bio,
            ImageId avatar,
            ImageId banner,
            Instant createdAt,
            Instant updatedAt) {

        /** What a user who never saved anything looks like: no name, empty bio, no images, no timestamps. */
        public static ProfileView defaultsFor(UserId userId) {
            return new ProfileView(userId, null, Bio.of(""), null, null, null, null);
        }
    }
}
