package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RegisterCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;

public interface RegisterUserUseCase extends UseCaseHandler<RegisterCommand, Void> {
}
