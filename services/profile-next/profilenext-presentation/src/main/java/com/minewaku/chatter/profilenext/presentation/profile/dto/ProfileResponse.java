package com.minewaku.chatter.profilenext.presentation.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import java.time.Instant;

/** Nulls are written, not omitted: a profile that was never saved reads as defaults (contracts/profile-api.md). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProfileResponse(
        long userId,
        String displayName,
        String bio,
        String avatarUrl,
        String bannerUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static ProfileResponse from(GetProfileUseCase.Result result) {
        return new ProfileResponse(
                result.userId(),
                result.displayName(),
                result.bio(),
                result.avatarUrl(),
                result.bannerUrl(),
                result.createdAt(),
                result.updatedAt());
    }
}
