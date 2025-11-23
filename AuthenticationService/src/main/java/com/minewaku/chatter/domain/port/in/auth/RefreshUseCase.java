package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.response.TokenResponse;

public interface RefreshUseCase extends UseCaseHandler<String, TokenResponse> {
}
