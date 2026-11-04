package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

public interface RemoveObsoleteImagesUseCase
        extends UseCaseHandler<RemoveObsoleteImagesUseCase.Command, RemoveObsoleteImagesUseCase.Result> {

    record Command() {}

    record Result(int removed, int failed, int reported) {}
}
