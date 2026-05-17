package com.minewaku.chatter.message.application.port.inbound.command.channel;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface CreateChannelUseCase extends UseCaseHandler<CreateChannelUseCase.Command, Void> {
    
    record Command(
        GuildId guildId,
        UserId requesterId,
        String name,
        String description
    ) {}
}
