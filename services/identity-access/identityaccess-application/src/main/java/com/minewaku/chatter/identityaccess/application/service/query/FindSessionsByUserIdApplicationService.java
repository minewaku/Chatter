package com.minewaku.chatter.identityaccess.application.service.query;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.port.inbound.query.FindSessionsByUserIdUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.SessionReadRepository;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class FindSessionsByUserIdApplicationService implements FindSessionsByUserIdUseCase {

    private final SessionReadRepository sessionReadRepository;

    public FindSessionsByUserIdApplicationService(
            SessionReadRepository sessionReadRepository) {

        this.sessionReadRepository = sessionReadRepository;
    }

    @Override
    @Retry(name = "transientDataAccess")
    @Transactional(readOnly = true)
    public Set<SessionReadModel> handle(FindSessionsByUserIdUseCase.Command command) {
        return sessionReadRepository.findAllSessionsByUserId(new UserId(command.userId()));
    }
}
