package com.minewaku.chatter.domain.command.user_role;

import com.minewaku.chatter.domain.value.id.RoleId;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record CreateUserRoleCommand(
    @NonNull UserId userId, 
    @NonNull RoleId roleId, 
    UserId createdBy
) {}

