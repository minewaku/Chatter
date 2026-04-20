package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

public interface LogoutOtherSessionsUseCase extends UseCaseHandler<LogoutOtherSessionsUseCase.Command, Void>{
    
    public record Command(
        Password password
    ) {} 
}
