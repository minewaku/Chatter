package com.minewaku.chatter.profilenext.presentation.image.dto;

import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase;
import java.util.Locale;

/** {@code outcome} is {@code applied} for a new image and {@code already_applied} for a repeat. */
public record CommitUploadResponse(String slot, String outcome, String imageId, String imageUrl) {

    public static CommitUploadResponse from(CommitImageUploadUseCase.Result result, String imageUrl) {
        return new CommitUploadResponse(
                result.slot().toLowerCase(Locale.ROOT),
                result.outcome().name().toLowerCase(Locale.ROOT),
                result.imageId(),
                imageUrl);
    }
}
