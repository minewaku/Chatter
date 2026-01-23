package com.minewaku.chatter.domain.command.role;

import com.minewaku.chatter.domain.value.id.RoleId;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record UpdateRoleCommand(
    @NonNull RoleId id, 
    @NonNull String name, 
    @NonNull String description
) {}
