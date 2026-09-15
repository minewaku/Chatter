package com.minewaku.chatter.identityaccess.application.port.inbound.query;

import java.util.Set;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;

public interface FindSessionsByUserIdUseCase extends UseCaseHandler<FindSessionsByUserIdUseCase.Command, Set<SessionReadModel>> {

    record Command(Long userId) {
    }
}
