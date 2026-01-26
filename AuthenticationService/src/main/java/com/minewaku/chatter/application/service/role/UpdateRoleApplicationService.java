package com.minewaku.chatter.application.service.role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.command.role.UpdateRoleCommand;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.in.role.UpdateRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class UpdateRoleApplicationService implements UpdateRoleUseCase {
	
	final RoleRepository roleRepository;
	
	
	public UpdateRoleApplicationService(RoleRepository roleRepository) {
		
		this.roleRepository = roleRepository;
	}

    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
    public Void handle(UpdateRoleCommand command) {
		Role role = roleRepository.findById(command.id())
				.orElseThrow(() -> new EntityNotFoundException("Role does not exist"));
				
		role.updateName(command.name());
		role.updateDescription(command.description());
		
        roleRepository.update(role);
        return null;
	}

}
