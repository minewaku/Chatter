package com.minewaku.chatter.identityaccess.authentication.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface RefreshAccessTokenUseCase
        extends UseCaseHandler<RefreshAccessTokenUseCase.Command, RefreshAccessTokenUseCase.Refreshed> {

    record Command(String refreshToken) {}

    record Refreshed(String accessToken) {}
}
