package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.value.id.OpaqueToken;

public interface LogoutUseCase extends UseCaseHandler<OpaqueToken, Void> {
}
