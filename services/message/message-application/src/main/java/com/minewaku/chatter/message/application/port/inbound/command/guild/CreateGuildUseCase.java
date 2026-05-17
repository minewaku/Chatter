package com.minewaku.chatter.message.application.port.inbound.command.guild;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface CreateGuildUseCase extends UseCaseHandler<CreateGuildUseCase.Command, Void>{

    public record Command(
        UserId requesterId,
        String name,
        String description
    ) {}
}