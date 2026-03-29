package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.command.InputBanner;
import com.minewaku.chatter.profile.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface UploadBannerUseCase extends UseCaseHandler<UploadBannerUseCase.Command, Void> {
    
    public record Command(
        ProfileId profileId,
        InputBanner banner
    ) {}
}
