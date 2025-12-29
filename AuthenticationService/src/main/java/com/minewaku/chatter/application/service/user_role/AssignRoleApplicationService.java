package com.minewaku.chatter.application.service.user_role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.command.user_role.CreateUserRoleCommand;
import com.minewaku.chatter.domain.model.UserRole;
import com.minewaku.chatter.domain.port.in.user_role.AssignRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.value.id.UserRoleId;

public class AssignRoleApplicationService implements AssignRoleUseCase {

	private final UserRoleRepository userRoleRepository;
	
	
	public AssignRoleApplicationService(UserRoleRepository userRoleRepository) {
		
		this.userRoleRepository = userRoleRepository;
	}
	
	
    @Override
	@Transactional
    public Void handle(CreateUserRoleCommand command) {
		UserRoleId userRoleId = new UserRoleId(command.getUserId(), command.getRoleId());
		UserRole userRole = UserRole.createNew(userRoleId, command.getCreatedBy());
        userRoleRepository.save(userRole);
        return null;
	}

}
