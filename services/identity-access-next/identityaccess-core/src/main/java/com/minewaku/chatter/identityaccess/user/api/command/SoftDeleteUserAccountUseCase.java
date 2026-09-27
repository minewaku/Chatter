package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface SoftDeleteUserAccountUseCase
        extends UseCaseHandler<SoftDeleteUserAccountUseCase.Command, SoftDeleteUserAccountUseCase.Result> {

    record Command(long userId) {}

    record Result(boolean changed) {}
}
