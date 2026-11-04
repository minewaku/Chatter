package com.minewaku.chatter.profilenext.profile.internal.application.query;

import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a profile; a user who never saved one gets the defaults, never an absence. */
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
        Profile profile = profiles.findByUserId(userId).orElseGet(() -> Profile.defaultsFor(userId));
        return new Result(
                profile.userId().value(),
                profile.displayName(),
                profile.bio(),
                urlOf(profile.avatar()),
                urlOf(profile.banner()),
                profile.createdAt(),
                profile.updatedAt());
    }

    private static String urlOf(StoredImage image) {
        return image == null ? null : image.url();
    }
}
