package com.minewaku.chatter.profile.application.port.inbound.command.security.usecase;

import com.minewaku.chatter.profile.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

public interface RestoreProfileUseCase extends UseCaseHandler<ProfileId, Void> {
}
