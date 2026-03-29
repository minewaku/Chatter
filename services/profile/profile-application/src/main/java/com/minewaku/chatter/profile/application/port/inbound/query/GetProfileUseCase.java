package com.minewaku.chatter.profile.application.port.inbound.query;

import com.minewaku.chatter.profile.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface GetProfileUseCase extends UseCaseHandler<ProfileId, ProfileReadModel>{
    
}
