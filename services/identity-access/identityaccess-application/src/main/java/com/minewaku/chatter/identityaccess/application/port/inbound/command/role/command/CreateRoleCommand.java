package com.minewaku.chatter.identityaccess.application.port.inbound.command.role.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.Code;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record CreateRoleCommand(
    @NonNull String name, 
    @NonNull Code code, 
    @NonNull String description
) {}

