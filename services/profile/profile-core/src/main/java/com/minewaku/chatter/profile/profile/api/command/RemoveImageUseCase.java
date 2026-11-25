package com.minewaku.chatter.profile.profile.api.command;

import com.minewaku.chatter.profile.shared.handler.UseCaseHandler;

public interface RemoveImageUseCase extends UseCaseHandler<RemoveImageUseCase.Command, RemoveImageUseCase.Result> {

    record Command(long userId, String slot) {}

    /** {@code removed} is false when the slot was already empty. */
    record Result(boolean removed) {}
}
