package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.DisplayName;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface ChangeDisplayNameUseCase extends UseCaseHandler<ChangeDisplayNameUseCase.Command, Void> {
    
    public record Command(
        ProfileId profileId,
        DisplayName displayName
    ) {}
}
