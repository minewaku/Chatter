package com.minewaku.chatter.application.service.role;

import com.minewaku.chatter.domain.command.role.UpdateRoleCommand;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.role.UpdateRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;

public class UpdateRoleApplicationService implements UpdateRoleUseCase {
	
	final RoleRepository roleRepository;
	
	
	public UpdateRoleApplicationService(RoleRepository roleRepository) {
		
		this.roleRepository = roleRepository;
	}

    @Override
    public Void handle(UpdateRoleCommand command) {
		Role role = roleRepository.findById(command.getId())
				.orElseThrow(() -> new IllegalArgumentException("Role does not exist"));
				
		role.setName(command.getName());
		role.setDescription(command.getDescription());
		
        roleRepository.update(role);
        return null;
	}

}
