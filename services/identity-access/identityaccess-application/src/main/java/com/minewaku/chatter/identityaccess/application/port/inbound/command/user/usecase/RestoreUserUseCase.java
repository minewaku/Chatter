package com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface RestoreUserUseCase extends UseCaseHandler<RestoreUserUseCase.Command, Void> {

    record Command(Long userId) {
    }
}
