package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RefreshTokenCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;

public interface RefreshUseCase extends UseCaseHandler<RefreshTokenCommand, TokenResponse> {
}
