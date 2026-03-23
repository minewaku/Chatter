package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.ChangePasswordCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;

public interface ChangePasswordUseCase extends UseCaseHandler<ChangePasswordCommand, Void> {
}
