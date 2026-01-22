package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.response.TokenResponse;
import com.minewaku.chatter.domain.value.id.OpaqueToken;

public interface RefreshUseCase extends UseCaseHandler<OpaqueToken, TokenResponse> {
}
