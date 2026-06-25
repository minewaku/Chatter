package com.minewaku.chatter.message.application.port.inbound.query;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.NonNull;

public interface CheckGuildAccessUseCase extends UseCaseHandler<CheckGuildAccessUseCase.Command, Boolean> {

    public record Command(
        @NonNull GuildId guildId,
        @NonNull UserId userId
    ) {}
}

