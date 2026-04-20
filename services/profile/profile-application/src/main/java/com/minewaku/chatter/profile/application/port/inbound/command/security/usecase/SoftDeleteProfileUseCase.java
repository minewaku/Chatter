package com.minewaku.chatter.profile.application.port.inbound.command.security.usecase;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.Enablement;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.model.Username;

import lombok.NonNull;

public interface SoftDeleteProfileUseCase extends UseCaseHandler<SoftDeleteProfileUseCase.Command, Void> {

    public record Command(
        @NonNull ProfileId profileId,
        @NonNull Username username,
        @NonNull Enablement enablement
    ) {}
}