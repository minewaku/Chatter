package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.Bio;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface ChangeBioUseCase extends UseCaseHandler<ChangeBioUseCase.Command, Void> {
    
    public record Command(
        ProfileId profileId,
        Bio bio
    ) {}
}
