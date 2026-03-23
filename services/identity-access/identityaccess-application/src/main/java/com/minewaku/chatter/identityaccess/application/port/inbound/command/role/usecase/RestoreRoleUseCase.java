package com.minewaku.chatter.identityaccess.application.port.inbound.command.role.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.RoleId;

public interface RestoreRoleUseCase extends UseCaseHandler<RoleId, Void> {
}
