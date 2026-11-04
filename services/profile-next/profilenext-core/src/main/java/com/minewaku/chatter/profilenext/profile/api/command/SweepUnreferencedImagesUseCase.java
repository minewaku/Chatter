package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

public interface SweepUnreferencedImagesUseCase
        extends UseCaseHandler<SweepUnreferencedImagesUseCase.Command, SweepUnreferencedImagesUseCase.Result> {

    record Command() {}

    record Result(int removed) {}
}
