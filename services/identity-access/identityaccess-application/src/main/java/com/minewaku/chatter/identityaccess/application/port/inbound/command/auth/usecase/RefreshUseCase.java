package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;

public interface RefreshUseCase extends UseCaseHandler<RefreshUseCase.Command, TokenResponse> {

    record Command(String refreshToken) {
    }
}
