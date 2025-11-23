package com.minewaku.chatter.domain.port.in.role;

import com.minewaku.chatter.domain.command.role.CreateRoleCommand;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface CreateRoleUseCase extends UseCaseHandler<CreateRoleCommand, Role> {
}
