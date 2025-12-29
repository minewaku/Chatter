package com.minewaku.chatter.application.service.role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.command.role.CreateRoleCommand;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.role.CreateRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.port.out.service.IdGenerator;
import com.minewaku.chatter.domain.value.id.RoleId;

public class CreateRoleApplicationService implements CreateRoleUseCase {

	private final IdGenerator idGenerator;
	private final RoleRepository roleRepository;

	public CreateRoleApplicationService(
			IdGenerator idGenerator,
			RoleRepository roleRepository) {

		this.idGenerator = idGenerator;
		this.roleRepository = roleRepository;
	}

	@Override
	@Transactional
	public Role handle(CreateRoleCommand command) {
		RoleId roleId = new RoleId(idGenerator.generate());

		Role role = Role.createNew(roleId, command.getName(),
				command.getCode(), command.getDescription());

		return roleRepository.save(role);
	}
}
