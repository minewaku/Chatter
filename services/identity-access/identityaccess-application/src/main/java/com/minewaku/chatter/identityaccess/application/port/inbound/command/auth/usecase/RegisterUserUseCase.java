package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import java.time.LocalDate;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface RegisterUserUseCase extends UseCaseHandler<RegisterUserUseCase.Command, Void> {

    record Command(
            String email,
            String username,
            LocalDate birthday,
            String password) {
    }
}
