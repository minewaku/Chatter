package com.minewaku.chatter.application.service.role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
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
	@Transactional
	public Void handle(RoleId roleId) {
		Role role = roleRepository.findById(roleId).orElseThrow(
				() -> new EntityNotFoundException("Role does not exist"));
		role.softDelete();

		roleRepository.softDelete(role);
		return null;
	}
}
