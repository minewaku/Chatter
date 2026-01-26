package com.minewaku.chatter.application.service.role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.role.RestoreRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.value.id.RoleId;

import io.github.resilience4j.retry.annotation.Retry;

public class RestoreRoleApplicationService implements RestoreRoleUseCase {

	final RoleRepository roleRepository;
	
	public RestoreRoleApplicationService(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}
	
	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public Void handle(RoleId roleId) {
		Role role = roleRepository.findById(roleId).orElseThrow(
				() -> new EntityNotFoundException("Role does not exist"));
		role.restore();
		
		roleRepository.restore(role);
		
		return null;
	}

}
