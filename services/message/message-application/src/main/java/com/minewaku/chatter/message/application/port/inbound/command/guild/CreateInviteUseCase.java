package com.minewaku.chatter.message.application.port.inbound.command.guild;

import java.time.Duration;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface CreateInviteUseCase extends UseCaseHandler<CreateInviteUseCase.Command, Void> {
    
    public record Command (
        GuildId guildId,
        UserId userId,
        Duration duration,
        int maxUses
    ) {
    }
}
