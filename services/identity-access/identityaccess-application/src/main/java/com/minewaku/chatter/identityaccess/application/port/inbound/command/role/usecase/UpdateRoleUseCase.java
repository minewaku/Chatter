package com.minewaku.chatter.identityaccess.application.port.inbound.command.role.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.role.command.UpdateRoleCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;

public interface UpdateRoleUseCase extends UseCaseHandler<UpdateRoleCommand, Void> {
}
