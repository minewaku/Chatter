package com.minewaku.chatter.identityaccess.authentication.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface LogoutOtherSessionsUseCase
        extends UseCaseHandler<LogoutOtherSessionsUseCase.Command, LogoutOtherSessionsUseCase.Result> {

    record Command(long userId, String currentSessionId, String currentPassword) {}

    record Result(int revokedCount) {}
}
