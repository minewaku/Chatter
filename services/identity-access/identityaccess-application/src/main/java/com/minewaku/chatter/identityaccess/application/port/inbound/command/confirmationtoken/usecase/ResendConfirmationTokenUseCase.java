package com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface ResendConfirmationTokenUseCase extends UseCaseHandler<ResendConfirmationTokenUseCase.Command, Void> {

    record Command(String email) {
    }
}
