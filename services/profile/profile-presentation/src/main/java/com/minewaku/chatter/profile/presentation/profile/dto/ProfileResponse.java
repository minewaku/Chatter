package com.minewaku.chatter.profile.presentation.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.minewaku.chatter.profile.presentation.image.ImageUrlResolver;
import com.minewaku.chatter.profile.profile.api.query.GetProfileUseCase;
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

    /** {@code urls} turns each image's relative path into its public address; an empty slot stays null. */
    public static ProfileResponse from(GetProfileUseCase.Result result, ImageUrlResolver urls) {
        return new ProfileResponse(
                result.userId(),
                result.displayName(),
                result.bio(),
                urlOf(result.avatar(), urls),
                urlOf(result.banner(), urls),
                result.createdAt(),
                result.updatedAt());
    }

    private static String urlOf(GetProfileUseCase.ImageResult image, ImageUrlResolver urls) {
        return image == null ? null : urls.resolve(image.path());
    }
}
