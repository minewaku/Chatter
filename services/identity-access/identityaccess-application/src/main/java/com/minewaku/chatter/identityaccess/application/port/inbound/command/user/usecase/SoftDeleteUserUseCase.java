package com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface SoftDeleteUserUseCase extends UseCaseHandler<UserId, Void> {
}
