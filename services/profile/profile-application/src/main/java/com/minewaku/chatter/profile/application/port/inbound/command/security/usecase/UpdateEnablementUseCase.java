package com.minewaku.chatter.profile.application.port.inbound.command.security.usecase;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.Enablement;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

import lombok.NonNull;

public interface UpdateEnablementUseCase extends UseCaseHandler<UpdateEnablementUseCase.Command, Void> {

    public record Command(
        @NonNull ProfileId profileId,
        @NonNull Enablement enablement
    ) {}
    
}
