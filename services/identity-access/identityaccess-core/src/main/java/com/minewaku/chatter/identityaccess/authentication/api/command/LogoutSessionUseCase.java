package com.minewaku.chatter.identityaccess.authentication.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface LogoutSessionUseCase
        extends UseCaseHandler<LogoutSessionUseCase.Command, LogoutSessionUseCase.Result> {

    record Command(long userId, String sessionId, String currentPassword) {}

    record Result(boolean revoked) {}
}
