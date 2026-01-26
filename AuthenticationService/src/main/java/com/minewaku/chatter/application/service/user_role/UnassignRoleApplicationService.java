package com.minewaku.chatter.application.service.user_role;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.port.in.user_role.UnassignRoleUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.value.id.UserRoleId;

import io.github.resilience4j.retry.annotation.Retry;

public class UnassignRoleApplicationService implements UnassignRoleUseCase {

    private final UserRoleRepository userRoleRepository;

    public UnassignRoleApplicationService(UserRoleRepository userRoleRepository) {

        this.userRoleRepository = userRoleRepository;
    }

    @Override
	@Retry(name = "transientDataAccess")
    @Transactional
    public Void handle(UserRoleId userRoleId) {
        userRoleRepository.deleteById(userRoleId);
        return null;
    }

}
