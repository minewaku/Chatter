package com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface UnlockUserUseCase extends UseCaseHandler<UserId, Void> {
}
