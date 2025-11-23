package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.service.role.CreateRoleApplicationService;
import com.minewaku.chatter.application.service.role.HardDeleteRoleApplicationService;
import com.minewaku.chatter.application.service.role.RestoreRoleApplicationService;
import com.minewaku.chatter.application.service.role.SoftDeleteRoleApplicationService;
import com.minewaku.chatter.application.service.role.UpdateRoleApplicationService;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.port.out.service.IdGenerator;

@Configuration
class RoleServiceConfig {

	@Bean
	CreateRoleApplicationService createRoleApplicationService(IdGenerator idGenerator, RoleRepository roleRepository) {
		return new CreateRoleApplicationService(idGenerator, roleRepository);
	}

	@Bean
	SoftDeleteRoleApplicationService softDeleteRoleApplicationService(RoleRepository roleRepository) {
		return new SoftDeleteRoleApplicationService(roleRepository);
	}
	
	@Bean
	HardDeleteRoleApplicationService hardDeleteRoleApplicationService(RoleRepository roleRepository) {
		return new HardDeleteRoleApplicationService(roleRepository);
	}

	@Bean
	UpdateRoleApplicationService updateRoleApplicationService(RoleRepository roleRepository) {
		return new UpdateRoleApplicationService(roleRepository);
	}

	@Bean
	RestoreRoleApplicationService restoreRoleApplicationService(RoleRepository roleRepository) {
		return new RestoreRoleApplicationService(roleRepository);
	}
}


