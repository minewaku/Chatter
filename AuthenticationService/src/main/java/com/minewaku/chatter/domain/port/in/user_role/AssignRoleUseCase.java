package com.minewaku.chatter.domain.port.in.user_role;

import com.minewaku.chatter.domain.command.user_role.CreateUserRoleCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface AssignRoleUseCase extends UseCaseHandler<CreateUserRoleCommand, Void> {
}
