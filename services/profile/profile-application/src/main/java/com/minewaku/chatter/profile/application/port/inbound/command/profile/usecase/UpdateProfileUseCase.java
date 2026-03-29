package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.Bio;
import com.minewaku.chatter.profile.domain.model.profile.model.DisplayName;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface UpdateProfileUseCase extends UseCaseHandler<UpdateProfileUseCase.Command, Void> {
    
    public record Command(
        ProfileId profileId,
        DisplayName displayName,
        Bio bio
    ) {}
}  
