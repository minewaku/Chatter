package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface UnlockUserAccountUseCase
        extends UseCaseHandler<UnlockUserAccountUseCase.Command, UnlockUserAccountUseCase.Result> {

    record Command(long userId) {}

    record Result(boolean changed) {}
}
