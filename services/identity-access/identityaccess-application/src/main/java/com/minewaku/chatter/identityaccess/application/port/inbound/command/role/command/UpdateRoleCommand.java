package com.minewaku.chatter.identityaccess.application.port.inbound.command.role.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.RoleId;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record UpdateRoleCommand(
    @NonNull RoleId id, 
    @NonNull String name, 
    @NonNull String description
) {}
