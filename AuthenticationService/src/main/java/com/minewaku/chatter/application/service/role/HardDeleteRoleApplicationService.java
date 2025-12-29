package com.minewaku.chatter.application.service.role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.port.in.role.HardDeleteRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.value.id.RoleId;

public class HardDeleteRoleApplicationService implements HardDeleteRoleUseCase {

	private final RoleRepository roleRepository;
	
	public HardDeleteRoleApplicationService(
			RoleRepository roleRepository) {
		
		this.roleRepository = roleRepository;
	}
	
    @Override
	@Transactional
    public Void handle(RoleId roleId) {
        roleRepository.hardDeleteById(roleId);
        return null;
	}
}
