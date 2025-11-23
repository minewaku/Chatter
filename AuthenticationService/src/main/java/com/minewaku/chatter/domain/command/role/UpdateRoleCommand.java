package com.minewaku.chatter.domain.command.role;

import com.minewaku.chatter.domain.value.id.RoleId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class UpdateRoleCommand {
	
    @NonNull
    private final RoleId id;
    
    @NonNull @Setter
    private String name;
    
    @NonNull @Setter
    private String description;
    
    public UpdateRoleCommand(@NonNull RoleId id, @NonNull String name, 
			@NonNull String description) {

		this.id = id;
		this.name = name;
		this.description = description;
	}
}
