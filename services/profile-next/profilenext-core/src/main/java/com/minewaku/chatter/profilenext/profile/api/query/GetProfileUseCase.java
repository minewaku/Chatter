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

    /** An image of the profile: its id and its provider-neutral relative path (never an address). */
    record ImageResult(String imageId, String path) {}
}
