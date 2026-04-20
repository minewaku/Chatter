package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.Enablement;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.model.Username;

public interface CreateProfileUseCase extends UseCaseHandler<CreateProfileUseCase.Command, Void> {
    
    record Command(
        ProfileId profileId,
        Username username,
        Enablement enablement
    ) {
        
    }
}