package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface LogoutSpecificSessionUseCase extends UseCaseHandler<LogoutSpecificSessionUseCase.Command, Void> {

    record Command(String sessionId, String password) {
    }
}
