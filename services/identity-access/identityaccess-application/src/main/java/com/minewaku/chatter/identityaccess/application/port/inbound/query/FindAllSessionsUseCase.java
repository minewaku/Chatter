package com.minewaku.chatter.identityaccess.application.port.inbound.query;

import java.util.Set;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface FindAllSessionsUseCase extends UseCaseHandler<UserId, Set<SessionReadModel>> {

}
