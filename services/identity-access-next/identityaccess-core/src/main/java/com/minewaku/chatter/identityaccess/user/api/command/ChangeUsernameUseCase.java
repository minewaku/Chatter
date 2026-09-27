package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface ChangeUsernameUseCase
        extends UseCaseHandler<ChangeUsernameUseCase.Command, ChangeUsernameUseCase.Result> {

    record Command(long userId, String currentPassword, String newUsername) {}

    record Result(boolean changed) {}
}
