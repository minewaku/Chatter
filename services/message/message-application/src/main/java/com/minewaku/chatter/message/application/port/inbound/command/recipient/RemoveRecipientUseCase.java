package com.minewaku.chatter.message.application.port.inbound.command.recipient;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface RemoveRecipientUseCase extends UseCaseHandler<RemoveRecipientUseCase.Command, Void> {
    
    record Command(
        GuildId guildId,
        UserId requesterId,
        UserId userId
    ) {}
}
