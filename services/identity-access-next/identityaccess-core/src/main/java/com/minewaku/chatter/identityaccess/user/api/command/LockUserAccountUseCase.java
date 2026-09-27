package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface LockUserAccountUseCase
        extends UseCaseHandler<LockUserAccountUseCase.Command, LockUserAccountUseCase.Result> {

    record Command(long userId) {}

    record Result(boolean changed) {}
}
