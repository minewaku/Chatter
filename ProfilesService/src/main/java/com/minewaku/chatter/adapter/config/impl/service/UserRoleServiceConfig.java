package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.service.user_role.AssignRoleApplicationService;
import com.minewaku.chatter.application.service.user_role.UnassignRoleApplicationService;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;

@Configuration
public class UserRoleServiceConfig {
	@Bean
	AssignRoleApplicationService assignRoleApplicationService(UserRoleRepository userRoleRepository) {
		return new AssignRoleApplicationService(userRoleRepository);
	}

	@Bean
	UnassignRoleApplicationService unassignRoleApplicationService(UserRoleRepository userRoleRepository) {
		return new UnassignRoleApplicationService(userRoleRepository);
	}
}


