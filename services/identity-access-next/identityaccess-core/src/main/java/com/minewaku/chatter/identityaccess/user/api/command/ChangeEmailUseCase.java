package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface ChangeEmailUseCase extends UseCaseHandler<ChangeEmailUseCase.Command, ChangeEmailUseCase.Result> {

    record Command(long userId, String currentPassword, String newEmail) {}

    record Result(boolean changed, boolean verificationRequired) {}
}
