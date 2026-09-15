package com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface UnlockUserUseCase extends UseCaseHandler<UnlockUserUseCase.Command, Void> {

    record Command(Long userId) {
    }
}
