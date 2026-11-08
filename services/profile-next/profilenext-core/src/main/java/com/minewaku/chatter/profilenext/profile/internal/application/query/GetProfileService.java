package com.minewaku.chatter.profilenext.profile.internal.application.query;

import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageRef;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository.ProfileView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a profile; a user who never saved one gets the defaults, never an absence. Images come back as an id and a
 * promoted flag: building an address from them is not the core's job.
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
                resultOf(profile.avatar()),
                resultOf(profile.banner()),
                profile.createdAt(),
                profile.updatedAt());
    }

    private static ImageResult resultOf(ImageRef image) {
        return image == null ? null : new ImageResult(image.id().value().toString(), image.promoted());
    }
}
