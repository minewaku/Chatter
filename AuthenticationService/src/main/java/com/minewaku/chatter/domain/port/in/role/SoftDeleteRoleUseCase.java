package com.minewaku.chatter.domain.port.in.role;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.value.id.RoleId;

public interface SoftDeleteRoleUseCase extends UseCaseHandler<RoleId, Void> {
}
