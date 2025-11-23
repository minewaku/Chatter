package com.minewaku.chatter.domain.command.user_role;

import com.minewaku.chatter.domain.value.id.RoleId;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CreateUserRoleCommand {

    @NonNull
    private final UserId userId;

    @NonNull
    private final RoleId roleId;

    private final UserId createdBy;

    public CreateUserRoleCommand(
    		@NonNull UserId userId, 
    		@NonNull RoleId roleId, 
    		UserId createdBy) {
    	
        this.userId = userId;
        this.roleId = roleId;
        this.createdBy = createdBy;
    }
}

