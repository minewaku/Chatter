package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LoginCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;

public interface LoginUseCase extends UseCaseHandler<LoginCommand, TokenResponse> {
}