package com.minewaku.chatter.message.application.port.inbound.command.invite;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.invite.model.Code;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface DeleteInviteUseCase extends UseCaseHandler<DeleteInviteUseCase.Command, Void> {
    
    public record Command (
        Code inviteCode,
        UserId requesterId
    ) {
    }
    
}
