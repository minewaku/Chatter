package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.application.shared.DeviceInfoDto;

public interface LoginUseCase extends UseCaseHandler<LoginUseCase.Command, TokenResponse> {

    record Command(
            String email,
            String password,
            DeviceInfoDto deviceInfo) {
    }
}