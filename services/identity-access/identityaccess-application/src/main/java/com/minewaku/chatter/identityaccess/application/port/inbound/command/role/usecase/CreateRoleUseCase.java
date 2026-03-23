package com.minewaku.chatter.identityaccess.application.port.inbound.command.role.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.role.command.CreateRoleCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.Role;

public interface CreateRoleUseCase extends UseCaseHandler<CreateRoleCommand, Role> {
}
