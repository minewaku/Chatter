package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.command.auth.RegisterCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface RegisterUseCase extends UseCaseHandler<RegisterCommand, Void> {
}
