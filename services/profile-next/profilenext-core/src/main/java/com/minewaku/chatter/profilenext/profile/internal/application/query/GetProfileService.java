package com.minewaku.chatter.profilenext.profile.internal.application.query;

import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImagePath;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository.ProfileView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a profile; a user who never saved one gets the defaults, never an absence. Images come back as an id and a
 * relative path: turning that into an address is not the core's job.
 */
@Service
@Transactional(readOnly = true)
public class GetProfileService implements GetProfileUseCase {

    private final ProfileQueryRepository profiles;

    public GetProfileService(ProfileQueryRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Result handle(Query query) {
        UserId userId = UserId.of(query.userId());
        ProfileView profile = profiles.findByUserId(userId).orElseGet(() -> ProfileView.defaultsFor(userId));
        return new Result(
                profile.userId().value(),
                profile.displayName() == null ? "" : profile.displayName().value(),
                profile.bio().value(),
                imageOf(userId, ImageSlot.AVATAR, profile.avatar()),
                imageOf(userId, ImageSlot.BANNER, profile.banner()),
                profile.createdAt(),
                profile.updatedAt());
    }

    private static ImageResult imageOf(UserId userId, ImageSlot slot, ImageId id) {
        return id == null ? null : new ImageResult(id.value().toString(), ImagePath.of(userId, slot, id));
    }
}
