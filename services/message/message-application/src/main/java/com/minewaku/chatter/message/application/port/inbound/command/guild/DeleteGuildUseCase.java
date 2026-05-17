package com.minewaku.chatter.message.application.port.inbound.command.guild;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface DeleteGuildUseCase extends UseCaseHandler<DeleteGuildUseCase.Command, Void>{
    
    public record Command(
        GuildId guildId,
        UserId requesterId
    ) {}
}