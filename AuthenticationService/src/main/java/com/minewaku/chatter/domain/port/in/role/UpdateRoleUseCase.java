package com.minewaku.chatter.domain.port.in.role;

import com.minewaku.chatter.domain.command.role.UpdateRoleCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface UpdateRoleUseCase extends UseCaseHandler<UpdateRoleCommand, Void> {
}
