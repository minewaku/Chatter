package com.minewaku.chatter.identityaccess.authentication.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface RefreshAccessTokenUseCase
        extends UseCaseHandler<RefreshAccessTokenUseCase.Command, RefreshAccessTokenUseCase.Refreshed> {

    record Command(String refreshToken) {}

    /** The new refresh credential replaces the presented one; it is returned only here. */
    record Refreshed(String accessToken, String refreshToken) {}
}
