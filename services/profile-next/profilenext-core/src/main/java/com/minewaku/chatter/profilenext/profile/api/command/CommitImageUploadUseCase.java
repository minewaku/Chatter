package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

public interface CommitImageUploadUseCase
        extends UseCaseHandler<CommitImageUploadUseCase.Command, CommitImageUploadUseCase.Result> {

    /** {@code userId} comes from the authenticated caller only. {@code imageId} is the id the upload ticket returned. */
    record Command(long userId, String slot, String imageId) {}

    /** {@code path} is the provider-neutral relative path of the image; the caller turns it into an address. */
    record Result(Outcome outcome, String imageId, String path) {}

    enum Outcome {
        /** The upload is now the slot's image. */
        APPLIED,
        /** The slot already held this image; nothing changed. */
        ALREADY_APPLIED
    }
}
