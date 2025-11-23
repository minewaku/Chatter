package com.minewaku.chatter.domain.command.role;

import com.minewaku.chatter.domain.value.Code;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CreateRoleCommand {

	@NonNull
    private final String name;
	
	@NonNull
    private final Code code; 
	
	@NonNull
    private final String description;

    public CreateRoleCommand(@NonNull String name, @NonNull Code code, 
    		@NonNull String description) {
    	
        this.name = name;
        this.code = code;
        this.description = description;
    }
}
