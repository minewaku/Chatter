package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.command.InputAvatar;
import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface UploadAvatarUseCase extends UseCaseHandler<UploadAvatarUseCase.Command, Void> {
    
    public record Command(
        ProfileId profileId,
        InputAvatar avatar
    ) {}
}
