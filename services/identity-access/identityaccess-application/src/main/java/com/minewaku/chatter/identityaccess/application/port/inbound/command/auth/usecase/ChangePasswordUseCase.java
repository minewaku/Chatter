package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface ChangePasswordUseCase extends UseCaseHandler<ChangePasswordUseCase.Command, Void> {

    record Command(Long userId, String password, String newPassword) {
    }
}
