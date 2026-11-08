package com.minewaku.chatter.profilenext.profile.api.query;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;
import java.time.Instant;

public interface GetProfileUseCase extends UseCaseHandler<GetProfileUseCase.Query, GetProfileUseCase.Result> {

    record Query(long userId) {}

    record Result(
            long userId,
            String displayName,
            String bio,
            ImageResult avatar,
            ImageResult banner,
            Instant createdAt,
            Instant updatedAt) {}

    /** An image of the profile: its id, and whether it has been moved to permanent storage yet. */
    record ImageResult(String imageId, boolean promoted) {}
}
