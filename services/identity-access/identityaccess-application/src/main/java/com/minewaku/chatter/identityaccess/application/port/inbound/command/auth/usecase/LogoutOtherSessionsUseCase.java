package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.ChangePasswordCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler.UseCaseHandler;

public interface LogoutOtherSessionsUseCase extends UseCaseHandler<ChangePasswordCommand, Void>{
    
}
