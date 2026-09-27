package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface SuspendUserAccountUseCase
        extends UseCaseHandler<SuspendUserAccountUseCase.Command, SuspendUserAccountUseCase.Result> {

    record Command(long userId) {}

    record Result(boolean changed) {}
}
