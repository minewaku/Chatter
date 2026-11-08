package com.minewaku.chatter.profilenext.profile.internal.port.query;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageRef;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ProfileQueryRepository {

    Optional<ProfileView> findByUserId(UserId userId);

    /** Of the given images, the ids that the stated owner's profile currently holds in the stated slot. */
    Set<ImageId> findReferenced(Collection<OwnedImage> images);

    /**
     * A profile as a reader sees it. An {@link ImageRef} is present when the slot holds an image; its {@code promoted}
     * flag is true exactly when no promotion is pending for that image.
     */
    record ProfileView(
            UserId userId,
            DisplayName displayName,
            Bio bio,
            ImageRef avatar,
            ImageRef banner,
            Instant createdAt,
            Instant updatedAt) {

        /** What a user who never saved anything looks like: no name, empty bio, no images, no timestamps. */
        public static ProfileView defaultsFor(UserId userId) {
            return new ProfileView(userId, null, Bio.of(""), null, null, null, null);
        }
    }
}
