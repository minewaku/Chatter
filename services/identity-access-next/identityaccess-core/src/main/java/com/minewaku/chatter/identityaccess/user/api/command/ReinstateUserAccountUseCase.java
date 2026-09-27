package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface ReinstateUserAccountUseCase
        extends UseCaseHandler<ReinstateUserAccountUseCase.Command, ReinstateUserAccountUseCase.Result> {

    record Command(long userId) {}

    record Result(boolean changed) {}
}
