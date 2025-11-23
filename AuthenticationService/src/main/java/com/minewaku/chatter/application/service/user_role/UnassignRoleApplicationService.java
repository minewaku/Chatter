package com.minewaku.chatter.application.service.user_role;

import com.minewaku.chatter.domain.port.in.user_role.UnassignRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.value.id.UserRoleId;

public class UnassignRoleApplicationService implements UnassignRoleUseCase {

	private final UserRoleRepository userRoleRepository;
	
	
	public UnassignRoleApplicationService(UserRoleRepository userRoleRepository) {
		
		this.userRoleRepository = userRoleRepository;
	}
	
	
    @Override
    public Void handle(UserRoleId userRoleId) {
        userRoleRepository.deleteById(userRoleId);
        return null;
    }

}
