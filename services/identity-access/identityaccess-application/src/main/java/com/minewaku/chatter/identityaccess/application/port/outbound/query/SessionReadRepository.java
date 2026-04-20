package com.minewaku.chatter.identityaccess.application.port.outbound.query;

import java.util.Set;

import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface SessionReadRepository {
    Set<SessionReadModel> findAllSessionsByUserId(UserId userId);
}
