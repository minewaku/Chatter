package com.minewaku.chatter.profilenext.profile.api.query;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;
import java.time.Instant;

public interface GetProfileUseCase extends UseCaseHandler<GetProfileUseCase.Query, GetProfileUseCase.Result> {

    record Query(long userId) {}

    record Result(
            long userId,
            String displayName,
            String bio,
            String avatarUrl,
            String bannerUrl,
            Instant createdAt,
            Instant updatedAt) {}
}
