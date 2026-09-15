package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface LogoutCurrentSessionUseCase extends UseCaseHandler<LogoutCurrentSessionUseCase.Command, Void> {

    record Command(String refreshToken) {
    }
}
