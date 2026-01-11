package com.minewaku.chatter.domain.port.in.user;

import com.minewaku.chatter.domain.command.profile.CreateUserCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface CreateUserUseCase extends UseCaseHandler<CreateUserCommand, Void> {
    
}
