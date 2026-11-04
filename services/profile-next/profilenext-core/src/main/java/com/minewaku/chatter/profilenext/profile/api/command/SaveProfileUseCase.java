package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

public interface SaveProfileUseCase extends UseCaseHandler<SaveProfileUseCase.Command, SaveProfileUseCase.Result> {

    /** {@code displayName} and {@code bio} are each null when the caller did not send them. */
    record Command(long userId, String displayName, String bio) {}

    record Result(boolean changed) {}
}
