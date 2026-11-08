package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

public interface ConfirmImageUploadUseCase
        extends UseCaseHandler<ConfirmImageUploadUseCase.Command, ConfirmImageUploadUseCase.Result> {

    /** The raw body must reach the signature check byte for byte. */
    record Command(byte[] rawBody, String signature, String timestamp) {}

    record Result(Outcome outcome) {}

    enum Outcome {
        /** The profile now shows the new image. */
        APPLIED,
        /** A repeat, an older upload, or an upload for a user who has no profile: nothing changed. */
        IGNORED,
        /** The file broke the format, size or dimension rule; nothing was written and the file is left to expire. */
        FILE_NOT_ALLOWED
    }
}
