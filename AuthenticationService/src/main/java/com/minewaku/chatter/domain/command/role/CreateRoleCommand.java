package com.minewaku.chatter.domain.command.role;

import com.minewaku.chatter.domain.value.Code;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record CreateRoleCommand(
    @NonNull String name, 
    @NonNull Code code, 
    @NonNull String description
) {}

