package com.minewaku.chatter.domain.port.in.confirmation_token;

import com.minewaku.chatter.domain.command.confirmation_token.CreateConfirmationTokenCommand;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface CreateConfirmationTokenUseCase extends UseCaseHandler<CreateConfirmationTokenCommand, ConfirmationToken> {
}
