package com.minewaku.chatter.application.service.role;

import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.role.SoftDeleteRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.value.id.RoleId;

public class SoftDeleteRoleApplicationService implements SoftDeleteRoleUseCase {
	
	final RoleRepository roleRepository;
	
	
	public SoftDeleteRoleApplicationService(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}


    @Override
    public Void handle(RoleId roleId) {
		Role role = roleRepository.findById(roleId).orElseThrow(
				() -> new IllegalArgumentException("Role does not exist"));
		role.setDeleted(true);
		
        roleRepository.update(role);
        return null;
	}
}
