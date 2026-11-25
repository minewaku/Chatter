package com.minewaku.chatter.profile.profile.internal.application.query;

import com.minewaku.chatter.profile.profile.api.query.GetProfileUseCase;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImagePath;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import com.minewaku.chatter.profile.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profile.profile.internal.port.query.ProfileQueryRepository.ProfileView;
import java.util.UUID;
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
        ProfileView profile = profiles.findByUserId(userId).orElseGet(() -> ProfileView.defaultsFor(userId.value()));
        return new Result(
                profile.userId(),
                profile.displayName(),
                profile.bio(),
                imageOf(userId, ImageSlot.AVATAR, profile.avatar()),
                imageOf(userId, ImageSlot.BANNER, profile.banner()),
                profile.createdAt(),
                profile.updatedAt());
    }

    private static ImageResult imageOf(UserId userId, ImageSlot slot, UUID id) {
        return id == null ? null : new ImageResult(id.toString(), ImagePath.of(userId, slot, new ImageId(id)));
    }
}
