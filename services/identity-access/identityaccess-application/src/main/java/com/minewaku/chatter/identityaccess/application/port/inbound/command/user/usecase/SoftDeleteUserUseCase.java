package com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface SoftDeleteUserUseCase extends UseCaseHandler<SoftDeleteUserUseCase.Command, Void> {

    record Command(Long userId) {
    }
}
