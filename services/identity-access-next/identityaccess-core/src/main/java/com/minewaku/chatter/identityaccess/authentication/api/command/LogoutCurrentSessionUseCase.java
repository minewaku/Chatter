package com.minewaku.chatter.identityaccess.authentication.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface LogoutCurrentSessionUseCase
        extends UseCaseHandler<LogoutCurrentSessionUseCase.Command, LogoutCurrentSessionUseCase.Result> {

    record Command(String refreshToken) {}

    record Result(boolean revoked) {}
}
