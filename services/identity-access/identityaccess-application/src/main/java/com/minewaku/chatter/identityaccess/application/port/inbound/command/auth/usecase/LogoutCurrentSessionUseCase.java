package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LogoutCurrentSessionCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;

public interface LogoutCurrentSessionUseCase extends UseCaseHandler<LogoutCurrentSessionCommand, Void> {
}
