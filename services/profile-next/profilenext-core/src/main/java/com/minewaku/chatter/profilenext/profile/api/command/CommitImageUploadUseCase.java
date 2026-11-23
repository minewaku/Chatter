package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

public interface CommitImageUploadUseCase
        extends UseCaseHandler<CommitImageUploadUseCase.Command, CommitImageUploadUseCase.Result> {

    /**
     * {@code userId} comes from the authenticated caller only. {@code imageId} is the id the upload ticket returned; the
     * ticket knows which slot it was issued for, so the caller does not name one.
     */
    record Command(long userId, String imageId) {}

    /**
     * {@code slot} is the slot the image belongs to. {@code path} is the provider-neutral relative path of the image;
     * the caller turns it into an address.
     */
    record Result(Outcome outcome, String slot, String imageId, String path) {}

    enum Outcome {
        /** The upload is now the slot's image. */
        APPLIED,
        /** The image was already the slot's image; nothing changed. */
        ALREADY_APPLIED
    }
}
