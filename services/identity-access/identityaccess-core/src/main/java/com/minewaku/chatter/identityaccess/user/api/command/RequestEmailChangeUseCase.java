package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface RequestEmailChangeUseCase
        extends UseCaseHandler<RequestEmailChangeUseCase.Command, RequestEmailChangeUseCase.Result> {

    record Command(long userId, String currentPassword, String newEmail) {}

    /** {@code requested} is false when the new email equals the current one and nothing was stored or sent. */
    record Result(boolean requested) {}
}
