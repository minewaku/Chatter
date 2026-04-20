package com.minewaku.chatter.identityaccess.application.service.query;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.application.port.inbound.query.FindAllSessionsUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.SessionReadRepository;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class FindAllSessionsApplicationService implements FindAllSessionsUseCase {
 
    private final SessionReadRepository sessionReadRepository;

    public FindAllSessionsApplicationService(
            SessionReadRepository sessionReadRepository) {

        this.sessionReadRepository = sessionReadRepository;
    }

    @Override
    @Retry(name = "transientDataAccess")
    public Set<SessionReadModel> handle(UserId userId) {
        Set<SessionReadModel> sessions = sessionReadRepository.findAllSessionsByUserId(userId);
        return sessions;
    }
}
