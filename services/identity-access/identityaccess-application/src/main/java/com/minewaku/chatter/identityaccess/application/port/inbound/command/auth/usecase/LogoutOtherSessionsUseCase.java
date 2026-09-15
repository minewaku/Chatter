package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface LogoutOtherSessionsUseCase extends UseCaseHandler<LogoutOtherSessionsUseCase.Command, Void> {

    record Command(String password) {
    }
}
