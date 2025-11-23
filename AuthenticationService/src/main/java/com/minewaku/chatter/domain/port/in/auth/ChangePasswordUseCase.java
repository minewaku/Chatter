package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.command.auth.ChangePasswordCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface ChangePasswordUseCase extends UseCaseHandler<ChangePasswordCommand, Void> {
}
