package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface ActivateAfterVerificationUseCase
        extends UseCaseHandler<ActivateAfterVerificationUseCase.Command, ActivateAfterVerificationUseCase.Result> {

    record Command(long userId) {}

    record Result(boolean changed) {}
}
