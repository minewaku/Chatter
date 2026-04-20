package com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;

public interface ResendConfirmationTokenUseCase extends UseCaseHandler<Email, Void> {
}
