package com.minewaku.chatter.application.service.role;

import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.role.RestoreRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.value.id.RoleId;

public class RestoreRoleApplicationService implements RestoreRoleUseCase {

	final RoleRepository roleRepository;
	
	public RestoreRoleApplicationService(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}
	
	@Override
	public Void handle(RoleId roleId) {
		Role role = roleRepository.findById(roleId).orElseThrow(
				() -> new IllegalArgumentException("Role does not exist"));
		role.setDeleted(false);
		
		roleRepository.update(role);
		
		return null;
	}

}
