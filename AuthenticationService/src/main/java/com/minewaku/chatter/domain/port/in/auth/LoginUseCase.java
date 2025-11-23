package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.command.auth.LoginCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.response.TokenResponse;

public interface LoginUseCase extends UseCaseHandler<LoginCommand, TokenResponse> {
}
