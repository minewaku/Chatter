package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface ChangePasswordUseCase
        extends UseCaseHandler<ChangePasswordUseCase.Command, ChangePasswordUseCase.Result> {

    record Command(long userId, String currentPassword, String newPassword) {}

    record Result(boolean changed) {}
}
